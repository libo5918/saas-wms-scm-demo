package com.example.scm.aiagent.rag.controller;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.rag.dto.RagChatRequest;
import com.example.scm.aiagent.rag.dto.RagChatResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertRequest;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertResponse;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.service.RagService;
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
 * RAG API 控制器。
 *
 * <p>提供文档写入、向量检索和最小 RAG Chat 接口，统一复用网关透传的租户和用户上下文。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 写入文档并生成切片向量。
     */
    @PostMapping("/documents")
    public Result<RagDocumentUpsertResponse> upsertDocument(@Valid @RequestBody RagDocumentUpsertRequest request,
                                                            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                            @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                            @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("RAG document upsert request received, tenantId={}, userId={}, knowledgeBaseId={}, documentId={}, contentLength={}",
                context.tenantId(), context.userId(), request.getKnowledgeBaseId(), request.getDocumentId(), safeLength(request.getContent()));
        return Result.success(ragService.upsertDocument(request, context));
    }

    /**
     * 基于 query 检索知识库切片。
     */
    @PostMapping("/retrieve")
    public Result<RagRetrieveResponse> retrieve(@Valid @RequestBody RagRetrieveRequest request,
                                                @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("RAG retrieve request received, tenantId={}, userId={}, knowledgeBaseId={}, topK={}, queryLength={}",
                context.tenantId(), context.userId(), request.getKnowledgeBaseId(), request.getTopK(), safeLength(request.getQuery()));
        return Result.success(ragService.retrieve(request, context));
    }

    /**
     * 执行最小 RAG Chat。
     */
    @PostMapping("/chat")
    public Result<RagChatResponse> ragChat(@Valid @RequestBody RagChatRequest request,
                                           @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                           @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                           @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("RAG chat request received, tenantId={}, userId={}, knowledgeBaseId={}, taskType={}, topK={}, messageLength={}",
                context.tenantId(), context.userId(), request.getKnowledgeBaseId(), request.getTaskType(), request.getTopK(),
                safeLength(request.getMessage()));
        return Result.success(ragService.ragChat(request, context));
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

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
