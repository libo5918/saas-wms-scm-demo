package com.example.scm.aiagent.agent.controller;

import com.example.scm.aiagent.agent.dto.AgentChatRequest;
import com.example.scm.aiagent.agent.dto.AgentChatResponse;
import com.example.scm.aiagent.agent.service.RagToolAgentChatService;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.Result;
import com.example.scm.common.core.TenantContext;
import com.example.scm.common.security.GatewayHeaders;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 企业级 Agent 组合问答入口。
 *
 * <p>Phase 5.1 提供 RAG + Tool 的统一演示接口，不替换已有 RAG 和 Tool Calling API。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/agent")
public class AgentChatController {

    private final RagToolAgentChatService agentChatService;

    public AgentChatController(RagToolAgentChatService agentChatService) {
        this.agentChatService = agentChatService;
    }

    @PostMapping("/chat")
    public Result<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request,
                                          @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                          @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                          @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI agent rag-tool chat request received, tenantId={}, userId={}, runId={}, knowledgeBaseId={}, messageLength={}",
                context.tenantId(), context.userId(), request.getRunId(), request.getKnowledgeBaseId(),
                request.getMessage() == null ? 0 : request.getMessage().length());
        return Result.success(agentChatService.chat(request, context));
    }

    private AgentRequestContext buildContext(Long userId, String username, String roles) {
        Long tenantId = TenantContext.getRequiredTenantId();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED.code(), "Missing user context");
        }
        return new AgentRequestContext(tenantId, userId, username, parseRoles(roles));
    }

    private List<String> parseRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
