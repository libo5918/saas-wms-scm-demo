package com.example.scm.aiagent.tool.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.tool.client.ToolCircuitOpenException;
import com.example.scm.aiagent.tool.client.ToolClientException;
import com.example.scm.aiagent.tool.model.ToolRuntimeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool 运行时保护服务。
 *
 * <p>负责 timeout 配置观测、可重试异常 retry、轻量熔断和运行状态统计。
 * 当前实现仅保存在内存中，不持久化请求参数、业务数据或敏感信息。</p>
 */
@Slf4j
@Service
public class ToolRuntimeProtectionService {

    private final AiAgentProperties properties;
    private final Map<String, RuntimeStats> statsByTool = new ConcurrentHashMap<>();

    public ToolRuntimeProtectionService(AiAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 在运行时保护策略下执行 Tool 调用。
     */
    public Object execute(String toolName, Callable<Object> action) {
        AiAgentProperties.RuntimeProperties runtime = properties.getTools().getRuntime();
        RuntimeStats stats = statsByTool.computeIfAbsent(toolName, RuntimeStats::new);
        stats.onCall();
        if (runtime.isCircuitBreakerEnabled() && stats.isOpen(runtime)) {
            log.warn("AI tool runtime circuit open, toolName={}, circuitState={}, timeoutMs={}",
                    toolName, stats.circuitState, runtime.getTimeoutMs());
            ToolCircuitOpenException exception = new ToolCircuitOpenException("Tool circuit is open: " + toolName);
            stats.onFailure(exception, runtime);
            throw exception;
        }

        int maxRetries = Math.max(0, runtime.getMaxRetries());
        int maxAttempts = runtime.isRetryEnabled() ? maxRetries + 1 : 1;
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startedAt = System.nanoTime();
            try {
                Object data = action.call();
                long latencyMs = elapsedMs(startedAt);
                if (runtime.getTimeoutMs() > 0 && latencyMs > runtime.getTimeoutMs()) {
                    throw new ToolClientException("Tool execution timed out after " + runtime.getTimeoutMs() + "ms");
                }
                log.info("AI tool runtime protected call finished, toolName={}, retryAttempt={}, timeoutMs={}, latencyMs={}",
                        toolName, attempt, runtime.getTimeoutMs(), latencyMs);
                stats.onSuccess();
                return data;
            } catch (RuntimeException ex) {
                lastException = ex;
                if (!shouldRetry(ex, attempt, maxAttempts, runtime)) {
                    stats.onFailure(ex, runtime);
                    throw ex;
                }
                stats.onRetry();
                log.warn("AI tool runtime retry scheduled, toolName={}, retryAttempt={}, maxRetries={}, timeoutMs={}, errorType={}",
                        toolName, attempt, maxRetries, runtime.getTimeoutMs(), ex.getClass().getSimpleName());
            } catch (Exception ex) {
                ToolClientException wrapped = new ToolClientException("Tool execution failed: " + ex.getMessage(), ex);
                stats.onFailure(wrapped, runtime);
                throw wrapped;
            }
        }
        RuntimeException finalException = lastException == null ? new ToolClientException("Tool execution failed") : lastException;
        stats.onFailure(finalException, runtime);
        throw finalException;
    }

    /**
     * 查询全部 Tool runtime 状态。
     */
    public List<ToolRuntimeStatus> listStatuses() {
        return statsByTool.values().stream()
                .map(RuntimeStats::snapshot)
                .sorted(Comparator.comparing(ToolRuntimeStatus::getToolName))
                .toList();
    }

    /**
     * 查询单个 Tool runtime 状态；未调用过的工具返回零值状态。
     */
    public ToolRuntimeStatus getStatus(String toolName) {
        return statsByTool.computeIfAbsent(toolName, RuntimeStats::new).snapshot();
    }

    private boolean shouldRetry(RuntimeException ex,
                                int attempt,
                                int maxAttempts,
                                AiAgentProperties.RuntimeProperties runtime) {
        return runtime.isRetryEnabled()
                && attempt < maxAttempts
                && ex instanceof ToolClientException;
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    /**
     * 单个 Tool 的内存态运行统计。
     */
    private static final class RuntimeStats {

        private final String toolName;
        private long totalCalls;
        private long successCount;
        private long failureCount;
        private long retryCount;
        private int consecutiveFailures;
        private Instant lastFailureAt;
        private String lastErrorType;
        private String circuitState = "CLOSED";
        private Instant openedAt;

        private RuntimeStats(String toolName) {
            this.toolName = toolName;
        }

        synchronized void onCall() {
            totalCalls++;
        }

        synchronized void onRetry() {
            retryCount++;
        }

        synchronized void onSuccess() {
            successCount++;
            consecutiveFailures = 0;
            circuitState = "CLOSED";
            openedAt = null;
        }

        synchronized void onFailure(RuntimeException ex, AiAgentProperties.RuntimeProperties runtime) {
            failureCount++;
            consecutiveFailures++;
            lastFailureAt = Instant.now();
            lastErrorType = ex.getClass().getSimpleName();
            if (ex instanceof ToolCircuitOpenException && "OPEN".equals(circuitState)) {
                return;
            }
            if (runtime.isCircuitBreakerEnabled()
                    && ("HALF_OPEN".equals(circuitState)
                    || consecutiveFailures >= Math.max(1, runtime.getFailureThreshold()))) {
                circuitState = "OPEN";
                openedAt = Instant.now();
            }
        }

        synchronized boolean isOpen(AiAgentProperties.RuntimeProperties runtime) {
            if (!runtime.isCircuitBreakerEnabled()) {
                return false;
            }
            Instant now = Instant.now();
            if ("OPEN".equals(circuitState)) {
                if (openedAt != null && Duration.between(openedAt, now).toMillis() >= runtime.getOpenDurationMs()) {
                    circuitState = "HALF_OPEN";
                    return false;
                }
                return true;
            }
            if (consecutiveFailures >= Math.max(1, runtime.getFailureThreshold())) {
                circuitState = "OPEN";
                openedAt = now;
                return true;
            }
            return false;
        }

        synchronized ToolRuntimeStatus snapshot() {
            return ToolRuntimeStatus.builder()
                    .toolName(toolName)
                    .totalCalls(totalCalls)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .retryCount(retryCount)
                    .lastFailureAt(lastFailureAt)
                    .lastErrorType(lastErrorType)
                    .circuitState(circuitState)
                    .openedAt(openedAt)
                    .build();
        }
    }
}
