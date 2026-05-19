# RAG Document Registry MySQL 持久化说明

## 1. 阶段目标

Phase 3.7 的目标是把 Phase 3.6 中的 Document Registry 和 Import Batch 从纯内存能力升级为可选 MySQL 持久化能力。

当前阶段使用 MyBatis Mapper + XML 承载 SQL 逻辑，只持久化文档治理元数据，不持久化向量本身：

- 文档 chunk 向量仍由 `InMemoryRagVectorStore` 或 `MilvusRagVectorStore` 承载。
- 文档列表、文档详情、导入批次和批次文档关联可以切换到 MySQL 保存。
- 默认是 `mysql`，单元测试会显式覆盖为 `in-memory`，确保 CI 不依赖真实 MySQL。

## 2. 为什么需要 MySQL Registry

只有 Milvus chunk 不够支撑企业级知识库治理。真实项目还需要回答：

- 当前知识库导入过哪些文档。
- 每个文档来自哪个目录、哪个批次。
- 重复导入后 chunk 数量和更新时间是否变化。
- 删除文档时是否同步删除了向量 chunk。
- 服务重启后文档治理信息是否仍可查询。

因此本阶段新增 MySQL Registry，用于保存 RAG 文档治理层元数据。

## 3. 表结构

SQL 脚本位置：

```text
deploy/sql/ai-agent-rag-registry.sql
```

包含三张表：

```text
rag_document_registry
rag_import_batch
rag_import_document
```

### 3.1 rag_document_registry

保存文档级治理元数据。

关键字段：

```text
tenant_id
knowledge_base_id
document_id
title
source
file_path
file_name
directory
import_source
chunk_count
deleted_count
embedding_mode
embedding_model
vector_store_mode
import_batch_id
metadata_json
imported_at
updated_at
created_at
```

唯一约束：

```text
tenant_id + knowledge_base_id + document_id
```

这个约束保证同一个文档重复导入时更新原记录，而不是插入重复文档。

### 3.2 rag_import_batch

保存每次 docs 导入批次。

关键字段：

```text
tenant_id
import_batch_id
user_id
knowledge_base_id
scan_root
file_count
imported_count
skipped_count
vector_store_mode
embedding_mode
embedding_model
started_at
finished_at
latency_ms
created_at
```

### 3.3 rag_import_document

保存导入批次和文档的关联。

关键字段：

```text
tenant_id
import_batch_id
knowledge_base_id
document_id
created_at
```

## 4. MyBatis 实现方式

代码结构：

```text
scm-ai-agent/src/main/java/com/example/scm/aiagent/rag/persistence/mapper/RagDocumentRegistryMapper.java
scm-ai-agent/src/main/resources/mapper/rag/RagDocumentRegistryMapper.xml
scm-ai-agent/src/main/java/com/example/scm/aiagent/rag/persistence/po/RagDocumentRecordPO.java
scm-ai-agent/src/main/java/com/example/scm/aiagent/rag/persistence/po/RagImportBatchPO.java
```

`MysqlRagDocumentRegistry` 只负责业务模型和 PO 转换、metadata JSON 序列化、事务边界和日志；具体 SQL 放在 MyBatis XML 中，便于后续维护复杂查询。

为了避免 MyBatis Starter 让所有测试或无数据库场景强依赖数据源，`scm-ai-agent` 使用专用条件配置：

```text
RagRegistryMysqlDataSourceConfiguration
```

只有 `ai.agent.rag.registry.mode=mysql` 时才创建：

- `DataSource`
- `SqlSessionFactory`
- `SqlSessionTemplate`
- `ragRegistryTransactionManager`

## 5. 配置切换

默认配置为 MySQL：

```yaml
ai:
  agent:
    rag:
      registry:
        mode: mysql
        mysql:
          url: jdbc:mysql://127.0.0.1:3306/scm_ai_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
          username: root
          password: ${AI_AGENT_RAG_REGISTRY_MYSQL_PASSWORD:}
```

如果本地只是临时无数据库启动，可以显式切换为内存：

```yaml
ai:
  agent:
    rag:
      registry:
        mode: in-memory
```

也可以使用环境变量：

```text
AI_AGENT_RAG_REGISTRY_MODE=mysql
AI_AGENT_RAG_REGISTRY_MYSQL_URL=jdbc:mysql://127.0.0.1:3306/scm_ai_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
AI_AGENT_RAG_REGISTRY_MYSQL_USERNAME=root
AI_AGENT_RAG_REGISTRY_MYSQL_PASSWORD=你的本地密码
```

注意：真实密码不要提交到 Git。

## 6. IDEA 本地验证方式

### 6.1 准备数据库

在本地 MySQL 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS scm_ai_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

执行脚本：

```text
deploy/sql/ai-agent-rag-registry.sql
```

### 6.2 application-local.yml

`application-local.yml` 已默认使用 MySQL Registry：

```yaml
ai:
  agent:
    rag:
      registry:
        mode: mysql
```

并确认 `registry.mysql.url / username / password` 是你本地 MySQL 的连接信息。

### 6.3 启动服务

IDEA 中启动：

```text
ScmAiAgentApplication
```

Active profiles 填：

```text
local
```

如果同时通过 gateway 验证，需要启动：

```text
ScmGatewayApplication
ScmAuthApplication
ScmAiAgentApplication
```

## 7. Gateway 18080 验证接口

下面接口都通过 gateway `18080` 调用。请求前先登录获取 `accessToken`，然后带上：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

### 7.1 导入 docs

```http
POST http://localhost:18080/api/v1/ai/rag/import/docs
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

MySQL 验证 SQL：

```sql
SELECT tenant_id, knowledge_base_id, document_id, title, chunk_count, import_batch_id
FROM rag_document_registry
ORDER BY updated_at DESC;

SELECT tenant_id, import_batch_id, knowledge_base_id, imported_count
FROM rag_import_batch
ORDER BY started_at DESC;
```

### 7.2 查询文档列表

```http
GET http://localhost:18080/api/v1/ai/rag/documents?knowledgeBaseId=kb-project-docs
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

### 7.3 查询文档详情

```http
GET http://localhost:18080/api/v1/ai/rag/documents/{documentId}?knowledgeBaseId=kb-project-docs
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
    "importBatchId": "uuid"
  }
}
```

### 7.4 查询导入批次列表

```http
GET http://localhost:18080/api/v1/ai/rag/import/batches
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

### 7.5 查询导入批次详情

```http
GET http://localhost:18080/api/v1/ai/rag/import/batches/{importBatchId}
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
    "documentIds": ["doc-xxx"]
  }
}
```

### 7.6 删除文档

```http
DELETE http://localhost:18080/api/v1/ai/rag/documents/{documentId}?knowledgeBaseId=kb-project-docs
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

删除后可用下面 SQL 确认 Registry 记录已删除，历史批次记录仍保留：

```sql
SELECT document_id, deleted, deleted_at
FROM rag_document_registry
WHERE tenant_id = 1
  AND knowledge_base_id = 'kb-project-docs'
  AND document_id = 'doc-xxx';

SELECT *
FROM rag_import_batch
ORDER BY started_at DESC;
```

说明：

- `rag_document_registry` 使用逻辑删除，`deleted=1` 后不会再出现在文档列表和详情查询中。
- `rag_import_batch` 和 `rag_import_document` 是历史导入审计数据，不随文档删除一起删除。
- VectorStore 中的 chunk 会物理删除，避免已经删除的文档继续参与 RAG 检索。

## 8. 当前边界

本阶段不实现：

- Tools
- MCP
- Workflow
- 多 Agent
- 长任务编排
- 向量持久化到 MySQL
- 文档全文持久化到 MySQL

当前 MySQL 只保存治理元数据；RAG 检索仍通过 VectorStore 完成。
