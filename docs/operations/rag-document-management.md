# RAG 文档管理与导入元数据说明

## 1. 阶段目标

Phase 3.6 的目标是给 RAG 知识库补齐可治理的文档管理能力。

当前已经可以把文档切片后写入 in-memory 或 Milvus，但仅有向量 chunk 不够支撑企业级使用。真实项目还需要知道：

- 当前知识库有哪些文档
- 每个文档来自哪里
- 文档属于哪个导入批次
- 重复导入时更新了多少 chunk
- 删除文档时是否同步删除了向量数据

本阶段新增 Document Registry 和 Import Batch，作为后续 Tools、MCP、Workflow、多 Agent 使用知识库的基础。

## 2. 当前实现

### 2.1 Document Registry

新增 `RagDocumentRegistry` 抽象，当前默认实现为 `InMemoryRagDocumentRegistry`。

记录的文档元数据包括：

```text
tenantId
knowledgeBaseId
documentId
title
source
filePath
fileName
directory
importSource
chunkCount
deletedCount
embeddingMode
embeddingModel
vectorStoreMode
importBatchId
importedAt
updatedAt
metadata
```

### 2.2 Import Batch

docs 自动导入时会生成 `importBatchId`，同一批导入成功的文档会保存相同批次 ID。

批次元数据包括：

```text
tenantId
userId
importBatchId
knowledgeBaseId
scanRoot
fileCount
importedCount
skippedCount
vectorStoreMode
embeddingMode
embeddingModel
documentIds
startedAt
finishedAt
latencyMs
```

### 2.3 存储模式

当前默认存储模式已经调整为 `mysql`，用于避免服务重启后文档治理元数据丢失。

仍保留 `in-memory` 模式，原因是：

- 单元测试不依赖真实数据库、Milvus、Embedding API 或外部网络
- 本地没有 MySQL 时仍可临时启动和验证 RAG 主链路
- 排查问题时可以快速隔离数据库影响

Phase 3.7 已补充可选 MySQL 持久化实现：

```yaml
ai:
  agent:
    rag:
      registry:
        mode: mysql
```

详细说明见：

```text
docs/operations/rag-registry-mysql.md
```

## 3. 删除联动规则

文档删除接口会同时执行两件事：

1. 调用 `RagVectorStore.deleteByDocument(tenantId, knowledgeBaseId, documentId)` 删除向量 chunk。
2. 调用 `RagDocumentRegistry.deleteDocument(tenantId, knowledgeBaseId, documentId)` 删除文档治理记录。

MySQL Registry 下第 2 步是逻辑删除：`rag_document_registry.deleted = 1`。

删除必须带上 `tenantId + knowledgeBaseId + documentId`，避免跨租户或跨知识库误删。

`rag_import_batch` 和 `rag_import_document` 属于导入历史审计数据，不随文档删除一起删除。

## 4. Gateway 验证接口

下面接口默认通过 gateway `18080` 调用，调用前需要先登录并携带 `Authorization: Bearer <access_token>`。

### 4.1 导入 docs 并生成批次

```http
POST http://localhost:18080/api/v1/ai/rag/import/docs
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{
  "knowledgeBaseId": "kb-project-docs",
  "maxFiles": 5
}
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "knowledgeBaseId": "kb-project-docs",
    "importBatchId": "uuid",
    "importedCount": 5,
    "documents": [
      {
        "documentId": "doc-xxx",
        "importBatchId": "uuid",
        "chunkCount": 3
      }
    ]
  }
}
```

### 4.2 查询知识库文档列表

```http
GET http://localhost:18080/api/v1/ai/rag/documents?knowledgeBaseId=kb-project-docs
Authorization: Bearer <access_token>
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "knowledgeBaseId": "kb-project-docs",
    "documentCount": 1,
    "documents": [
      {
        "documentId": "doc-xxx",
        "title": "SkyWalking 接入说明",
        "source": "docs/operations/skywalking-integration.md",
        "chunkCount": 5,
        "embeddingMode": "dashscope",
        "vectorStoreMode": "milvus",
        "importBatchId": "uuid"
      }
    ]
  }
}
```

### 4.3 查询文档详情

```http
GET http://localhost:18080/api/v1/ai/rag/documents/{documentId}?knowledgeBaseId=kb-project-docs
Authorization: Bearer <access_token>
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "documentId": "doc-xxx",
    "title": "SkyWalking 接入说明",
    "filePath": "docs/operations/skywalking-integration.md",
    "directory": "docs/operations",
    "importSource": "docs-auto-import",
    "importedAt": "2026-05-20T00:00:00Z",
    "updatedAt": "2026-05-20T00:00:00Z"
  }
}
```

### 4.4 删除文档

```http
DELETE http://localhost:18080/api/v1/ai/rag/documents/{documentId}?knowledgeBaseId=kb-project-docs
Authorization: Bearer <access_token>
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "knowledgeBaseId": "kb-project-docs",
    "documentId": "doc-xxx",
    "registryDeleted": true,
    "deletedChunkCount": 5
  }
}
```

删除后再次调用 retrieve，如果只过滤该文档相关条件，应不再返回该文档的 chunk。

### 4.5 查询导入批次列表

```http
GET http://localhost:18080/api/v1/ai/rag/import/batches
Authorization: Bearer <access_token>
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "batchCount": 1,
    "batches": [
      {
        "importBatchId": "uuid",
        "knowledgeBaseId": "kb-project-docs",
        "importedCount": 5,
        "documentIds": ["doc-xxx"]
      }
    ]
  }
}
```

### 4.6 查询导入批次详情

```http
GET http://localhost:18080/api/v1/ai/rag/import/batches/{importBatchId}
Authorization: Bearer <access_token>
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "importBatchId": "uuid",
    "knowledgeBaseId": "kb-project-docs",
    "fileCount": 5,
    "importedCount": 5,
    "skippedCount": 0,
    "documentIds": ["doc-xxx"]
  }
}
```

## 5. 当前边界

本阶段不实现：

- Tools
- MCP
- Workflow
- 多 Agent
- 长任务编排
- 复杂文档版本 diff

这些会放到后续阶段继续推进。
