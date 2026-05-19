package com.example.scm.aiagent.rag.controller;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.rag.dto.RagChatRequest;
import com.example.scm.aiagent.rag.dto.RagChatResponse;
import com.example.scm.aiagent.rag.dto.RagDocsImportRequest;
import com.example.scm.aiagent.rag.dto.RagDocsImportResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentDeleteResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentListResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentRecordResponse;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertRequest;
import com.example.scm.aiagent.rag.dto.RagDocumentUpsertResponse;
import com.example.scm.aiagent.rag.dto.RagImportBatchListResponse;
import com.example.scm.aiagent.rag.dto.RagImportBatchResponse;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.service.RagDocsImportService;
import com.example.scm.aiagent.rag.service.RagService;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.Result;
import com.example.scm.common.core.TenantContext;
import com.example.scm.common.security.GatewayHeaders;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final RagDocsImportService ragDocsImportService;

    public RagController(RagService ragService, RagDocsImportService ragDocsImportService) {
        this.ragService = ragService;
        this.ragDocsImportService = ragDocsImportService;
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

    /**
     * 手动触发 docs Markdown 文档导入 RAG 知识库。
     */
    @PostMapping("/import/docs")
    public Result<RagDocsImportResponse> importDocs(@RequestBody(required = false) RagDocsImportRequest request,
                                                    @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                    @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                    @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        RagDocsImportRequest safeRequest = request == null ? new RagDocsImportRequest() : request;
        log.info("RAG docs import request received, tenantId={}, userId={}, knowledgeBaseId={}, scanRoot={}, maxFiles={}",
                context.tenantId(), context.userId(), safeRequest.getKnowledgeBaseId(), safeRequest.getScanRoot(),
                safeRequest.getMaxFiles());
        return Result.success(ragDocsImportService.importDocs(safeRequest, context));
    }

    /**
     * 查询知识库文档列表。
     */
    @GetMapping("/documents")
    public Result<RagDocumentListResponse> listDocuments(@RequestParam("knowledgeBaseId") String knowledgeBaseId,
                                                         @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                         @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                         @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("RAG document list request received, tenantId={}, userId={}, knowledgeBaseId={}",
                context.tenantId(), context.userId(), knowledgeBaseId);
        return Result.success(ragService.listDocuments(knowledgeBaseId, context));
    }

    /**
     * 查询文档详情。
     */
    @GetMapping("/documents/{documentId}")
    public Result<RagDocumentRecordResponse> getDocument(@PathVariable("documentId") String documentId,
                                                         @RequestParam("knowledgeBaseId") String knowledgeBaseId,
                                                         @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                         @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                         @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("RAG document detail request received, tenantId={}, userId={}, knowledgeBaseId={}, documentId={}",
                context.tenantId(), context.userId(), knowledgeBaseId, documentId);
        return Result.success(ragService.getDocument(knowledgeBaseId, documentId, context));
    }

    /**
     * 删除文档记录并联动删除向量 chunk。
     */
    @DeleteMapping("/documents/{documentId}")
    public Result<RagDocumentDeleteResponse> deleteDocument(@PathVariable("documentId") String documentId,
                                                            @RequestParam("knowledgeBaseId") String knowledgeBaseId,
                                                            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                            @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                            @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("RAG document delete request received, tenantId={}, userId={}, knowledgeBaseId={}, documentId={}",
                context.tenantId(), context.userId(), knowledgeBaseId, documentId);
        return Result.success(ragService.deleteDocument(knowledgeBaseId, documentId, context));
    }

    /**
     * 查询 docs 导入批次列表。
     */
    @GetMapping("/import/batches")
    public Result<RagImportBatchListResponse> listImportBatches(@RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                                @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                                @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("RAG import batch list request received, tenantId={}, userId={}",
                context.tenantId(), context.userId());
        return Result.success(ragService.listImportBatches(context));
    }

    /**
     * 查询 docs 导入批次详情。
     */
    @GetMapping("/import/batches/{importBatchId}")
    public Result<RagImportBatchResponse> getImportBatch(@PathVariable("importBatchId") String importBatchId,
                                                         @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                         @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                         @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("RAG import batch detail request received, tenantId={}, userId={}, importBatchId={}",
                context.tenantId(), context.userId(), importBatchId);
        return Result.success(ragService.getImportBatch(importBatchId, context));
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
