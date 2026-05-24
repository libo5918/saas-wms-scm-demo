package com.example.scm.aiagent.multiagent.service;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.model.MultiAgentPlan;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.rag.dto.RagRetrievedChunk;
import com.example.scm.aiagent.rag.service.RagService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/** KnowledgeAgent 执行器，复用现有 RAG retrieve 并只输出脱敏摘要。 */
@Service
public class MultiAgentKnowledgeService {

    private final RagService ragService;

    public MultiAgentKnowledgeService(RagService ragService) {
        this.ragService = ragService;
    }

    public Map<String, Object> retrieve(MultiAgentChatRequest request, AgentRequestContext context,
                                        MultiAgentPlan plan, boolean enabled) {
        if (!plan.isNeedRag()) {
            return skipped("Planner 未要求检索知识库");
        }
        if (!enabled) {
            return skipped("Multi-Agent RAG 能力未启用");
        }
        if (!StringUtils.hasText(request.getKnowledgeBaseId())) {
            return skipped("缺少 knowledgeBaseId，KnowledgeAgent 跳过 RAG retrieve");
        }

        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId(request.getKnowledgeBaseId());
        retrieveRequest.setQuery(request.getMessage());
        retrieveRequest.setTopK(request.getTopK());
        retrieveRequest.setScoreThreshold(request.getScoreThreshold());
        if (request.getFilters() != null) {
            retrieveRequest.setFilters(request.getFilters());
        }
        RagRetrieveResponse response = ragService.retrieve(retrieveRequest, context);
        return Map.of(
                "status", "SUCCESS",
                "knowledgeBaseId", response.getKnowledgeBaseId(),
                "retrievedCount", response.getRetrievedCount(),
                "latencyMs", response.getLatencyMs(),
                "chunks", response.getChunks().stream().map(this::toChunkSummary).toList()
        );
    }

    private Map<String, Object> skipped(String reason) {
        return Map.of("status", "SKIPPED", "skipReason", reason, "retrievedCount", 0, "chunks", List.of());
    }

    private Map<String, Object> toChunkSummary(RagRetrievedChunk chunk) {
        return Map.of(
                "documentId", safe(chunk.getDocumentId()),
                "chunkId", safe(chunk.getChunkId()),
                "title", safe(chunk.getTitle()),
                "source", safe(chunk.getSource()),
                "contentSnippet", snippet(chunk.getContent(), 300),
                "score", chunk.getScore()
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String snippet(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String sanitized = value
                .replaceAll("(?i)authorization\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)cookie\\s*[:=]\\s*\\S+", "[REDACTED]")
                .replaceAll("(?i)token\\s*[:=]\\s*\\S+", "[REDACTED]");
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}
