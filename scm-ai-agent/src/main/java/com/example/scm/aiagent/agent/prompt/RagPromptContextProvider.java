package com.example.scm.aiagent.agent.prompt;

import com.example.scm.aiagent.agent.dto.AgentRagChunkView;
import com.example.scm.aiagent.agent.dto.AgentRagView;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索结果 Provider。
 */
@Component
public class RagPromptContextProvider implements AgentPromptContextProvider {

    @Override
    public List<AgentPromptSection> provide(AgentPromptBuildRequest request) {
        AgentRagView rag = request.getRag();
        Map<String, Object> structuredData = new LinkedHashMap<>();
        structuredData.put("knowledgeBaseId", rag == null ? null : rag.getKnowledgeBaseId());
        structuredData.put("retrievedCount", rag == null ? 0 : rag.getRetrievedCount());
        structuredData.put("chunks", rag == null || rag.getChunks() == null ? List.of() : rag.getChunks().stream()
                .map(this::toChunkContext)
                .toList());
        return List.of(AgentPromptSection.builder()
                .type(AgentPromptContextType.RAG_CONTEXT)
                .source(AgentPromptContextSource.RAG)
                .title("知识库片段")
                .content(rag == null || rag.getRetrievedCount() <= 0 ? "未召回知识库片段。" : "以下是检索到的知识库片段摘要。")
                .structuredData(structuredData)
                .priority(20)
                .maxLength(2000)
                .included(true)
                .build());
    }

    private Map<String, Object> toChunkContext(AgentRagChunkView chunk) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("documentId", chunk.getDocumentId());
        context.put("chunkId", chunk.getChunkId());
        context.put("title", chunk.getTitle());
        context.put("source", chunk.getSource());
        context.put("contentSnippet", chunk.getContentSnippet());
        context.put("score", chunk.getScore());
        return context;
    }
}
