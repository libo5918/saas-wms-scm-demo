package com.example.scm.aiagent.rag.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.model.ChatModelResult;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.rag.dto.RagDocsImportRequest;
import com.example.scm.aiagent.rag.dto.RagDocsImportResponse;
import com.example.scm.aiagent.rag.dto.RagRetrieveRequest;
import com.example.scm.aiagent.rag.dto.RagRetrieveResponse;
import com.example.scm.aiagent.service.AgentChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagDocsImportServiceTest {

    @TempDir(cleanup = CleanupMode.NEVER)
    private Path tempDir;

    @Test
    void shouldScanMarkdownFilesAndImportIntoInMemoryVectorStore() throws Exception {
        Path docsRoot = tempDir.resolve("docs");
        Files.createDirectories(docsRoot.resolve("architecture"));
        Files.createDirectories(docsRoot.resolve("business"));
        Files.writeString(docsRoot.resolve("architecture").resolve("ai-agent.md"),
                "# AI Agent 路线\n\nRAG 阶段需要扫描 docs 文档并写入知识库。", StandardCharsets.UTF_8);
        Files.writeString(docsRoot.resolve("business").resolve("inventory.md"),
                "库存知识库\n\n库存余额查询需要携带 tenantId。", StandardCharsets.UTF_8);
        Files.writeString(docsRoot.resolve("business").resolve("ignored.txt"),
                "不是 Markdown 文件", StandardCharsets.UTF_8);

        RagService ragService = createRagService();
        AiAgentProperties properties = createProperties(docsRoot);
        RagDocsImportService importService = new RagDocsImportService(properties, ragService);
        AgentRequestContext context = new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));

        RagDocsImportResponse response = importService.importDocs(new RagDocsImportRequest(), context);

        assertEquals(2, response.getFileCount());
        assertEquals(2, response.getImportedCount());
        assertEquals(0, response.getSkippedCount());
        assertEquals("kb-project-docs", response.getKnowledgeBaseId());
        assertEquals("in-memory", response.getVectorStoreMode());
        assertEquals("mock", response.getEmbeddingMode());
        assertEquals("AI Agent 路线", response.getDocuments().get(0).getTitle());
        assertTrue(response.getDocuments().get(0).getSource().endsWith("architecture/ai-agent.md"));

        RagRetrieveRequest retrieveRequest = new RagRetrieveRequest();
        retrieveRequest.setKnowledgeBaseId("kb-project-docs");
        retrieveRequest.setQuery("RAG 文档导入");
        retrieveRequest.setTopK(3);
        RagRetrieveResponse retrieveResponse = ragService.retrieve(retrieveRequest, context);
        assertFalse(retrieveResponse.getChunks().isEmpty());
    }

    @Test
    void shouldGenerateStableDocumentIdAndExtractFallbackTitle() throws Exception {
        Path docsRoot = tempDir.resolve("docs");
        Files.createDirectories(docsRoot.resolve("operations"));
        Path file = docsRoot.resolve("operations").resolve("milvus-local-setup.md");
        Files.writeString(file, "没有一级标题\n\nMilvus 本地搭建说明。", StandardCharsets.UTF_8);

        RagDocsImportService importService = new RagDocsImportService(createProperties(docsRoot), createRagService());

        String firstId = importService.stableDocumentId("operations/milvus-local-setup.md");
        String secondId = importService.stableDocumentId("operations/milvus-local-setup.md");
        String otherId = importService.stableDocumentId("architecture/ai-agent-roadmap.md");

        assertEquals(firstId, secondId);
        assertNotEquals(firstId, otherId);
        assertEquals("milvus-local-setup", importService.extractTitle(Files.readString(file), file));
        assertEquals(1, importService.scanMarkdownFiles(docsRoot, List.of("operations"), Set.of(".md")).size());
    }

    private AiAgentProperties createProperties(Path docsRoot) {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getRag().getChunk().setSize(40);
        properties.getRag().getChunk().setOverlap(8);
        properties.getRag().getDocsImport().setRootPath(docsRoot.toString());
        properties.getRag().getDocsImport().setKnowledgeBaseId("kb-project-docs");
        properties.getRag().getDocsImport().setIncludeDirectories(List.of("architecture", "business", "operations"));
        properties.getRag().getDocsImport().setSupportedExtensions(List.of(".md"));
        properties.getRag().getDocsImport().setMaxFiles(10);
        return properties;
    }

    private RagService createRagService() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getRag().getChunk().setSize(40);
        properties.getRag().getChunk().setOverlap(8);
        MockRagEmbeddingClient embeddingClient = new MockRagEmbeddingClient(properties);
        SimpleRagDocumentChunker chunker = new SimpleRagDocumentChunker(properties, embeddingClient);
        InMemoryRagVectorStore vectorStore = new InMemoryRagVectorStore();
        AgentChatService agentChatService = new AgentChatService(
                properties,
                request -> new ModelRoute("qwen-plus", "qwen-plus", "dashscope", "dashscope", "mock",
                        "task_type:rag_qa", List.of("CHAT", "RAG"), List.of("qwen-turbo")),
                invocation -> new ChatModelResult("mock rag answer")
        );
        return new RagService(properties, chunker, embeddingClient, vectorStore, agentChatService);
    }
}
