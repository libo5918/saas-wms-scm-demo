package com.example.scm.aiagent.tool.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.tool.client.ToolClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;

/**
 * Tool 运行时保护服务。
 *
 * <p>当前阶段实现低风险 retry 和 timeout 语义保护，不引入新的第三方熔断依赖。</p>
 */
@Slf4j
@Service
public class ToolRuntimeProtectionService {

    private final AiAgentProperties properties;

    public ToolRuntimeProtectionService(AiAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 在运行时保护策略下执行 Tool 调用。
     */
    public Object execute(String toolName, Callable<Object> action) {
        AiAgentProperties.RuntimeProperties runtime = properties.getTools().getRuntime();
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
                return data;
            } catch (RuntimeException ex) {
                lastException = ex;
                if (!shouldRetry(ex, attempt, maxAttempts, runtime)) {
                    throw ex;
                }
                log.warn("AI tool runtime retry scheduled, toolName={}, retryAttempt={}, maxRetries={}, timeoutMs={}, errorType={}",
                        toolName, attempt, maxRetries, runtime.getTimeoutMs(), ex.getClass().getSimpleName());
            } catch (Exception ex) {
                throw new ToolClientException("Tool execution failed: " + ex.getMessage(), ex);
            }
        }
        throw lastException == null ? new ToolClientException("Tool execution failed") : lastException;
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
}
