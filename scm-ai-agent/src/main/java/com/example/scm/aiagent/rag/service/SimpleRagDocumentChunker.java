package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.rag.model.RagDocument;
import com.example.scm.aiagent.rag.model.RagDocumentChunk;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认文档切片器。
 *
 * <p>采用固定长度加重叠窗口策略，适合先跑通 RAG 最小闭环；后续可替换为 Markdown 结构化切片。</p>
 */
@Component
public class SimpleRagDocumentChunker implements RagDocumentChunker {

    private final AiAgentProperties properties;
    private final RagEmbeddingClient embeddingClient;

    public SimpleRagDocumentChunker(AiAgentProperties properties, RagEmbeddingClient embeddingClient) {
        this.properties = properties;
        this.embeddingClient = embeddingClient;
    }

    @Override
    public List<RagDocumentChunk> chunk(RagDocument document) {
        int chunkSize = Math.max(100, properties.getRag().getChunk().getSize());
        int overlap = Math.max(0, Math.min(properties.getRag().getChunk().getOverlap(), chunkSize / 2));
        String content = document.getContent() == null ? "" : document.getContent().trim();
        List<RagDocumentChunk> chunks = new ArrayList<>();
        if (content.isBlank()) {
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + chunkSize);
            String chunkText = content.substring(start, end).trim();
            if (!chunkText.isBlank()) {
                chunks.add(RagDocumentChunk.builder()
                        .tenantId(document.getTenantId())
                        .knowledgeBaseId(document.getKnowledgeBaseId())
                        .documentId(document.getDocumentId())
                        .chunkId(document.getDocumentId() + "-chunk-" + chunkIndex)
                        .chunkIndex(chunkIndex)
                        .content(chunkText)
                        .embedding(embeddingClient.embed(chunkText))
                        .title(document.getTitle())
                        .source(document.getSource())
                        .metadata(document.getMetadata())
                        .createdAt(Instant.now())
                        .build());
                chunkIndex++;
            }
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }
}
