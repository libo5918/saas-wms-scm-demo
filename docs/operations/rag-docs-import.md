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

## 9. Phase 3.5：重复导入清理机制

从 Phase 3.5 开始，docs 导入复用的 `RagService.upsertDocument` 会在写入前先按文档删除旧 chunk。

这样重复执行：

```http
POST http://localhost:18080/api/v1/ai/rag/import/docs
```

不会持续累积重复 chunk。相同 `documentId` 的旧数据会先被清理，再写入最新切片。

它主要解决三个问题：

- 相同文档重复导入导致 Milvus 或 in-memory 中出现脏数据。
- 文档内容变短后，旧的尾部 chunk 仍然能被检索到。
- 调整 chunk size、overlap 或 embedding 模型后，旧 chunk 干扰新结果。

接口关键返回字段中，单文档写入接口会返回：

```json
{
  "data": {
    "documentId": "doc-demo",
    "chunkCount": 2,
    "deletedCount": 2,
    "vectorStoreMode": "milvus",
    "embeddingMode": "dashscope"
  }
}
```

`deletedCount` 表示本次写入前清理掉的旧 chunk 数量。第一次写入通常为 `0`，重复导入同一文档时会大于等于 `0`。

## 10. Phase 3.5：scoreThreshold 与上下文裁剪

检索接口新增可选字段 `scoreThreshold`：

```json
{
  "knowledgeBaseId": "kb-project-docs",
  "query": "SkyWalking 接入说明",
  "topK": 5,
  "scoreThreshold": 0.2,
  "filters": {
    "directory": "docs/operations",
    "importSource": "docs-auto-import"
  }
}
```

说明：

- 不传 `scoreThreshold` 时使用配置 `ai.agent.rag.retrieval.score-threshold`。
- 默认值为 `0`，表示不过滤低分结果。
- 如果设置过高，可能返回 `retrievedCount=0`，这是正常现象。

RAG Chat 会继续返回完整 citations，但拼接进模型 prompt 的上下文会受以下配置限制：

```yaml
ai.agent.rag.retrieval.max-context-chunks: 5
ai.agent.rag.retrieval.max-context-chunk-length: 1200
```

这样可以避免一次检索返回过多或单个 chunk 太长，导致 prompt 过大、响应变慢或费用增加。

## 11. Phase 3.5 Gateway 验证示例

### 11.1 首次写入文档

```http
POST http://localhost:18080/api/v1/ai/rag/documents
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{
  "knowledgeBaseId": "kb-project-docs",
  "documentId": "doc-phase35-demo",
  "title": "Phase 3.5 Demo",
  "source": "manual/phase35-demo.md",
  "content": "Phase 3.5 实现了重导入清理、scoreThreshold 和 RAG Chat 上下文裁剪。",
  "metadata": {
    "directory": "manual",
    "importSource": "manual-test"
  }
}
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "documentId": "doc-phase35-demo",
    "chunkCount": 1,
    "deletedCount": 0
  }
}
```

### 11.2 重复写入同一文档

再次请求同一个 `documentId=doc-phase35-demo`，把 `content` 改短或改成新内容。

关键预期返回：

```json
{
  "success": true,
  "data": {
    "documentId": "doc-phase35-demo",
    "chunkCount": 1,
    "deletedCount": 1
  }
}
```

### 11.3 检索并使用 scoreThreshold

```http
POST http://localhost:18080/api/v1/ai/rag/retrieve
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{
  "knowledgeBaseId": "kb-project-docs",
  "query": "Phase 3.5 做了什么",
  "topK": 5,
  "scoreThreshold": 0,
  "filters": {
    "importSource": "manual-test"
  }
}
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "knowledgeBaseId": "kb-project-docs",
    "retrievedCount": 1,
    "chunks": [
      {
        "documentId": "doc-phase35-demo",
        "score": 0.5
      }
    ]
  }
}
```

### 11.4 RAG Chat 验证引用仍返回

```http
POST http://localhost:18080/api/v1/ai/rag/chat
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{
  "knowledgeBaseId": "kb-project-docs",
  "message": "Phase 3.5 做了什么？",
  "taskType": "rag_qa",
  "providerMode": "mock",
  "requestedModel": "qwen-plus",
  "topK": 5,
  "scoreThreshold": 0,
  "filters": {
    "importSource": "manual-test"
  }
}
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "chat": {
      "providerMode": "mock",
      "taskType": "rag_qa"
    },
    "retrievalCount": 1,
    "citations": [
      {
        "documentId": "doc-phase35-demo"
      }
    ]
  }
}
```