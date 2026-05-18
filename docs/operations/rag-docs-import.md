# RAG docs 自动导入说明

## 1. 目标

Phase 3.2 提供当前项目 `docs` 目录 Markdown 文档的手动导入能力，用于把架构文档、业务文档、运维文档和数据库文档写入 RAG 知识库。

本阶段默认仍然使用：

- `ai.agent.rag.embedding.mode=mock`
- `ai.agent.rag.vector-store.mode=in-memory`

因此本地验证不依赖真实 Milvus、真实 Embedding API 或外部网络。

## 2. 导入范围

默认扫描 `docs` 根目录下的以下子目录：

- `docs/architecture`
- `docs/business`
- `docs/operations`
- `docs/database`

默认只导入 `.md` 文件。

## 3. 配置项

```yaml
ai:
  agent:
    rag:
      docs-import:
        enabled: true
        root-path: docs
        knowledge-base-id: kb-project-docs
        include-directories: architecture,business,operations,database
        supported-extensions: .md
        max-files: 100
```

环境变量覆盖方式：

```text
AI_AGENT_RAG_DOCS_IMPORT_ENABLED=true
AI_AGENT_RAG_DOCS_ROOT=docs
AI_AGENT_RAG_DOCS_KNOWLEDGE_BASE_ID=kb-project-docs
AI_AGENT_RAG_DOCS_INCLUDE_DIRECTORIES=architecture,business,operations,database
AI_AGENT_RAG_DOCS_SUPPORTED_EXTENSIONS=.md
AI_AGENT_RAG_DOCS_MAX_FILES=100
```

## 4. 导入流程

1. 通过 gateway 调用 `POST /api/v1/ai/rag/import/docs`。
2. `scm-ai-agent` 根据配置解析 docs 根目录和允许扫描的子目录。
3. 扫描 Markdown 文件并按路径稳定排序。
4. 为每个文件生成稳定 `documentId`。
5. 标题优先取 Markdown 一级标题；没有一级标题时使用文件名。
6. `source` 使用相对路径，方便后续引用展示。
7. metadata 写入 `filePath`、`fileName`、`directory`、`extension`、`importSource`。
8. 复用 `RagService.upsertDocument` 完成切片、mock embedding 和向量写入。

## 5. 接口调用

启用 gateway 后，统一调用 18080 端口：

```http
POST http://localhost:18080/api/v1/ai/rag/import/docs
Authorization: Bearer <access_token>
Content-Type: application/json
```

请求体示例：

```json
{
  "knowledgeBaseId": "kb-project-docs",
  "scanRoot": "docs",
  "includeDirectories": ["architecture", "business", "operations", "database"],
  "supportedExtensions": [".md"],
  "maxFiles": 20
}
```

关键返回字段：

```json
{
  "success": true,
  "data": {
    "tenantId": 1,
    "userId": 10001,
    "knowledgeBaseId": "kb-project-docs",
    "scanRoot": "docs",
    "fileCount": 10,
    "importedCount": 10,
    "skippedCount": 0,
    "vectorStoreMode": "in-memory",
    "embeddingMode": "mock",
    "documents": [
      {
        "documentId": "doc-docs-architecture-ai-agent-roadmap-md-xxxxxxxxxxxx",
        "title": "AI Agent 建设路线图",
        "source": "docs/architecture/ai-agent-roadmap.md",
        "chunkCount": 3
      }
    ]
  }
}
```

## 6. 默认 in-memory 验证方式

1. 启动 `scm-auth`、`scm-gateway`、`scm-ai-agent`。
2. 登录获取 token。
3. 调用 `POST /api/v1/ai/rag/import/docs` 导入项目文档。
4. 调用 `POST /api/v1/ai/rag/retrieve`，使用 `knowledgeBaseId=kb-project-docs` 检索导入内容。
5. 调用 `POST /api/v1/ai/rag/chat`，确认 RAG Chat 可以基于导入文档返回引用。

## 7. 后续切换 Milvus

后续接入真实 Milvus 时，导入接口不需要改变，只需要切换向量存储配置：

```text
AI_AGENT_RAG_VECTOR_STORE_MODE=milvus
MILVUS_URI=http://localhost:19530
MILVUS_TOKEN=<local_token_if_needed>
MILVUS_COLLECTION_NAME=scm_ai_rag_chunks
```

切换后，`RagService.upsertDocument` 会通过 `RagVectorStore` 接口写入 `MilvusRagVectorStore`，docs 导入服务仍然只负责扫描和转换文档。

## 8. 当前阶段边界

本阶段不实现：

- 启动时自动导入
- 真实 Embedding API
- 真实 Milvus 写入强依赖
- Tools
- MCP
- Workflow
- 多 Agent
- 长任务编排
