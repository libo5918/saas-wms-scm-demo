# AI Agent Phase 3 RAG 基础能力说明

## 1. 本阶段目标

本阶段进入正式 Phase 3，实现 RAG 最小可运行闭环：

- 文档写入
- 文档切片
- mock embedding
- in-memory vector store
- 基于 query 的向量检索
- RAG Chat 拼接检索上下文后复用现有模型路由与 Chat 调用链路

本阶段保留 Milvus 作为真实向量数据库方向，但默认不连接 Milvus，不依赖真实 Embedding API，不访问外部网络。

## 2. 当前实现范围

当前落在 `scm-ai-agent` 模块内，后续能力稳定后可按路线图拆分出独立 `scm-ai-rag` 模块。

新增接口前缀：

```text
/api/v1/ai/rag
```

接口列表：

```text
POST /api/v1/ai/rag/documents
POST /api/v1/ai/rag/retrieve
POST /api/v1/ai/rag/chat
```

## 3. 核心链路

```mermaid
flowchart LR
    A[写入文档] --> B[固定窗口切片]
    B --> C[mock embedding]
    C --> D[in-memory vector store]
    E[用户问题] --> F[mock query embedding]
    F --> G[租户 + 知识库过滤检索]
    G --> H[拼接 RAG 上下文]
    H --> I[ModelRouter]
    I --> J[RoutingChatModelClient]
```

## 4. 租户隔离

当前 RAG 数据写入和检索都强制依赖网关透传上下文：

- `X-Tenant-Id`
- `X-User-Id`
- `X-User-Name`
- `X-User-Roles`

in-memory vector store 内部按以下 scope 分桶：

```text
tenantId + knowledgeBaseId
```

这保证不同租户即使使用相同 `knowledgeBaseId`，也不会互相检索到对方的文档切片。

## 5. 默认配置

默认模式：

```yaml
ai:
  agent:
    rag:
      embedding:
        mode: mock
      vector-store:
        mode: in-memory
```

默认配置保证：

- 本地无 Milvus 也能启动
- 单元测试不依赖真实 Milvus
- 单元测试不依赖真实 Embedding API Key
- CI 不访问外部模型或外部网络

## 6. Milvus 配置骨架

Milvus 是本项目 RAG 主线向量数据库，但当前阶段只预留配置，不强制启用。

环境变量：

```text
AI_AGENT_RAG_VECTOR_STORE_MODE=milvus
MILVUS_URI=http://localhost:19530
MILVUS_TOKEN=本地环境变量
MILVUS_COLLECTION_NAME=scm_ai_rag_chunks
MILVUS_VECTOR_FIELD=embedding
MILVUS_METRIC_TYPE=COSINE
```

后续接入真实 Milvus 时需要补齐：

- collection schema
- vector field
- scalar metadata fields
- index 创建
- tenantId / knowledgeBaseId filter
- upsert/delete/search 实现

## 7. API 示例

### 7.1 写入文档

```http
POST http://localhost:18087/api/v1/ai/rag/documents
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-User-Name: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "knowledgeBaseId": "kb-project",
  "documentId": "doc-ai-agent-roadmap",
  "title": "AI Agent 路线图",
  "source": "docs/architecture/ai-agent-roadmap.md",
  "content": "这里放文档正文",
  "metadata": {
    "domain": "architecture"
  }
}
```

### 7.2 检索文档

```http
POST http://localhost:18087/api/v1/ai/rag/retrieve
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
```

```json
{
  "knowledgeBaseId": "kb-project",
  "query": "这个项目如何做 RAG 租户隔离？",
  "topK": 3
}
```

### 7.3 RAG Chat

```http
POST http://localhost:18087/api/v1/ai/rag/chat
Content-Type: application/json
X-Tenant-Id: 1
X-User-Id: 10001
X-User-Name: admin
X-User-Roles: ROLE_ADMIN
```

```json
{
  "knowledgeBaseId": "kb-project",
  "message": "这个项目如何做 RAG 租户隔离？",
  "taskType": "rag_qa",
  "providerMode": "mock",
  "requestedModel": "qwen-plus",
  "topK": 3
}
```

如果要调用真实 Qwen，可以在模型 Provider smoke test 已验证通过的前提下，将 `providerMode` 改成：

```json
{
  "providerMode": "spring-ai"
}
```

## 8. 日志与安全

当前日志记录：

- `tenantId`
- `userId`
- `knowledgeBaseId`
- `documentId`
- `chunkCount`
- `topK`
- `retrievedCount`
- `modelName`
- `provider`
- `latencyMs`

当前日志不会完整打印：

- 文档全文
- 用户 prompt 全文
- 模型响应全文
- API Key
- Milvus token

## 9. 本阶段刻意不做

本阶段不实现：

- 真实 Milvus Java Client
- 真实 EmbeddingModel 调用
- MySQL RAG metadata 持久化
- 文档批量导入任务
- Tools
- MCP
- Workflow
- 多 Agent
- 长任务编排

## 10. 下一步建议

下一阶段建议实现真实 Milvus adapter：

1. 增加 Milvus Java SDK 依赖。
2. 实现 collection 初始化和 schema 管理。
3. 实现 `MilvusRagVectorStore`。
4. 增加本地 docker-compose 或运维文档。
5. 增加 profile 隔离的 Milvus smoke test，默认 CI 仍不依赖 Milvus。
## 11. Phase 3.1 Milvus Adapter 接入

当前已新增 `MilvusRagVectorStore`，并通过条件装配控制启用时机：

```text
ai.agent.rag.vector-store.mode=in-memory  -> InMemoryRagVectorStore
ai.agent.rag.vector-store.mode=milvus     -> MilvusRagVectorStore
```

默认仍然是 `in-memory`，因此本地无 Milvus 时应用可启动，单元测试也不会连接真实 Milvus。

Milvus Adapter 使用官方 Java SDK，负责：

- 初始化 collection schema
- 创建向量索引
- 写入 chunk 向量
- 按 `tenant_id + knowledge_base_id` 过滤检索
- 返回引用字段：`document_id`、`chunk_id`、`source`、`title`、`content`

详细本地搭建和验证方式见：

```text
docs/operations/milvus-local-setup.md
```
## Phase 3.2：docs 目录手动导入 RAG 知识库

本阶段新增 `docs` Markdown 文档导入能力，目标是让当前项目自己的架构、业务、运维和数据库文档可以快速进入 RAG 知识库。

落地范围：

- 新增 `POST /api/v1/ai/rag/import/docs` 手动导入接口
- 默认扫描 `docs/architecture`、`docs/business`、`docs/operations`、`docs/database`
- 支持配置 docs 根目录、知识库 ID、支持后缀和单次最大导入文件数
- 文档标题优先取 Markdown 一级标题，否则使用文件名
- `source` 使用相对路径
- metadata 包含 `filePath`、`fileName`、`directory`、`extension`、`importSource`
- 复用 `RagService.upsertDocument` 完成切片、mock embedding 和向量写入

默认验证方式仍然是 `in-memory + mock embedding`，不依赖真实 Milvus、真实 Embedding API 或外部网络。

操作说明见：

```text
docs/operations/rag-docs-import.md
```

## Phase 3.3：Milvus 端到端 smoke 与 metadata filter

本阶段在 Phase 3.1 的 `MilvusRagVectorStore` 基础上，补齐真实 Milvus 端到端 smoke 验证能力。

目标：

- 默认仍然保持 `in-memory + mock embedding`
- 只有显式配置 `ai.agent.rag.vector-store.mode=milvus` 时才连接 Milvus
- 使用 mock embedding 写入 Milvus，先验证向量数据库链路，不接真实 Embedding
- 启动时检查 collection schema，避免旧 schema 导致隐性写入失败
- 使用 `upsert` 写入 chunk，保证重复导入同一文档时可以覆盖同一 `chunk_id`
- 检索时强制追加 `tenant_id + knowledge_base_id` 过滤
- 支持 `documentId`、`chunkId`、`chunkIndex`、`title`、`source`、`filePath`、`fileName`、`directory`、`extension`、`importSource` 等 metadata filter
- metadata filter 通过白名单表达式构建器生成，避免任意字符串拼接到 Milvus filter expression

当前 Milvus collection 字段：

```text
chunk_id
tenant_id
knowledge_base_id
document_id
chunk_index
title
source
file_path
file_name
directory
extension
import_source
content
embedding
```

本阶段边界：

- 不实现真实 Embedding API
- 不实现 Milvus 高可用部署
- 不实现 MySQL RAG metadata 持久化
- 不实现 Tools、MCP、Workflow、多 Agent 和长任务编排

Milvus 本地安装、IDEA 配置和 gateway 接口验证方式见：

```text
docs/operations/milvus-local-setup.md
```
