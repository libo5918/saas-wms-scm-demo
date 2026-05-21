package com.example.scm.aiagent.toolcalling.answer;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelInvocation;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.model.ModelRouteRequest;
import com.example.scm.aiagent.service.ChatModelClient;
import com.example.scm.aiagent.service.ModelRouter;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingAnswerSummaryResult;
import com.example.scm.aiagent.toolcalling.model.ToolCallingPlan;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Tool Calling 二阶段答案总结服务。
 *
 * <p>当前阶段支持两种 answerMode：template 直接复用服务端模板回答；spring-ai
 * 由真实模型基于工具执行结果总结最终中文答案。</p>
 */
@Slf4j
@Service
public class ToolCallingAnswerSummaryService {

    private static final List<String> REQUIRED_CAPABILITIES = List.of("CHAT");

    private final AiAgentProperties properties;
    private final ModelRouter modelRouter;
    private final ChatModelClient chatModelClient;
    private final ToolCallingAnswerBuilder templateAnswerBuilder;
    private final ToolCallingAnswerPromptBuilder promptBuilder;

    public ToolCallingAnswerSummaryService(AiAgentProperties properties,
                                           ModelRouter modelRouter,
                                           ChatModelClient chatModelClient,
                                           ToolCallingAnswerBuilder templateAnswerBuilder,
                                           ToolCallingAnswerPromptBuilder promptBuilder) {
        this.properties = properties;
        this.modelRouter = modelRouter;
        this.chatModelClient = chatModelClient;
        this.templateAnswerBuilder = templateAnswerBuilder;
        this.promptBuilder = promptBuilder;
    }

    /**
     * 根据当前配置生成最终返回给用户的答案。
     */
    public ToolCallingAnswerSummaryResult summarize(ToolCallingChatRequest request,
                                                    AgentRequestContext context,
                                                    ToolCallingPlan plan,
                                                    ToolCallingExecutionView execution,
                                                    String runId) {
        String answerMode = resolveAnswerMode();
        if (!"spring-ai".equalsIgnoreCase(answerMode)) {
            return templateResult(plan, execution, "template", false);
        }

        AiAgentProperties.SpringAiAnswerProperties answerProperties = properties.getToolCalling().getSpringAiAnswer();
        if (!answerProperties.isEnabled()) {
            return handleDisabledSpringAiAnswer(plan, execution, answerProperties);
        }

        long startedAt = System.nanoTime();
        Exception lastError = null;
        int maxRetries = Math.max(1, answerProperties.getMaxRetries());
        String taskType = StringUtils.hasText(answerProperties.getTaskType())
                ? answerProperties.getTaskType()
                : "tool_calling_answer";
        String prompt = promptBuilder.build(request.getMessage(), plan.selectedTool(), plan.toolArguments(), execution);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ModelRoute route = modelRouter.route(new ModelRouteRequest(
                        context.tenantId(),
                        context.userId(),
                        taskType,
                        null,
                        "spring-ai",
                        REQUIRED_CAPABILITIES,
                        null,
                        null
                ));
                log.info("AI tool calling answer summary started, tenantId={}, userId={}, runId={}, answerMode=spring-ai, attempt={}, modelName={}, provider={}, success={}",
                        context.tenantId(), context.userId(), runId, attempt, route.modelName(), route.provider(), execution.isSuccess());

                ChatModelResult result = chatModelClient.chat(new ChatModelInvocation(
                        runId,
                        prompt,
                        taskType,
                        context,
                        route
                ));
                String answer = normalizeAnswer(result.answer());
                if (!StringUtils.hasText(answer)) {
                    throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(),
                            "Spring AI answer summary returned empty content");
                }

                long latencyMs = elapsedMs(startedAt);
                log.info("AI tool calling answer summary finished, tenantId={}, userId={}, runId={}, answerMode=spring-ai, attempt={}, latencyMs={}",
                        context.tenantId(), context.userId(), runId, attempt, latencyMs);
                return ToolCallingAnswerSummaryResult.builder()
                        .answer(answer)
                        .answerMode("spring-ai")
                        .fallbackUsed(false)
                        .build();
            } catch (Exception ex) {
                lastError = ex;
                log.warn("AI tool calling answer summary failed, tenantId={}, userId={}, runId={}, answerMode=spring-ai, attempt={}, errorType={}, errorMessage={}",
                        context.tenantId(), context.userId(), runId, attempt, ex.getClass().getSimpleName(), ex.getMessage());
            }
        }

        if (answerProperties.isFallbackToTemplate()) {
            log.warn("AI tool calling answer summary fallback to template, tenantId={}, userId={}, runId={}, answerMode=spring-ai, errorType={}, errorMessage={}",
                    context.tenantId(), context.userId(), runId,
                    lastError == null ? "Unknown" : lastError.getClass().getSimpleName(),
                    lastError == null ? "unknown error" : lastError.getMessage());
            return templateResult(plan, execution, "template", true);
        }

        if (lastError instanceof BusinessException businessException) {
            throw businessException;
        }
        throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(),
                "Spring AI answer summary failed: " + (lastError == null ? "unknown error" : lastError.getMessage()));
    }

    private ToolCallingAnswerSummaryResult handleDisabledSpringAiAnswer(ToolCallingPlan plan,
                                                                        ToolCallingExecutionView execution,
                                                                        AiAgentProperties.SpringAiAnswerProperties answerProperties) {
        if (answerProperties.isFallbackToTemplate()) {
            return templateResult(plan, execution, "template", true);
        }
        throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                "Spring AI answer summary is disabled by configuration");
    }

    private ToolCallingAnswerSummaryResult templateResult(ToolCallingPlan plan,
                                                          ToolCallingExecutionView execution,
                                                          String answerMode,
                                                          boolean fallbackUsed) {
        return ToolCallingAnswerSummaryResult.builder()
                .answer(templateAnswerBuilder.buildAnswer(plan, execution))
                .answerMode(answerMode)
                .fallbackUsed(fallbackUsed)
                .build();
    }

    private String resolveAnswerMode() {
        String answerMode = properties.getToolCalling().getAnswerMode();
        return StringUtils.hasText(answerMode) ? answerMode : "template";
    }

    private String normalizeAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return answer;
        }
        return answer.trim();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
