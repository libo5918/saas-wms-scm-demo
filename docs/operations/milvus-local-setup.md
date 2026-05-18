# Milvus 本地搭建与验证说明

## 1. 使用场景

本文档用于 `scm-ai-agent` Phase 3.1 的真实 Milvus Adapter 验证。

当前项目默认仍然使用：

```text
ai.agent.rag.vector-store.mode=in-memory
```

只有显式配置为 `milvus` 时，才会创建 `MilvusRagVectorStore` 并连接 Milvus。

## 2. 推荐环境

本地推荐使用 Docker Desktop 启动 Milvus Standalone。

先确认 Docker 可用：

```bash
docker version
docker compose version
```

## 3. Milvus 获取方式

Milvus 镜像来自官方 Docker 镜像仓库。后续如果需要固定完整 compose 文件，可以直接按 Milvus 官方 standalone compose 模板落到项目 `deploy` 或 `docs/operations` 下。

当前阶段先记录运行要求，不强制提交本地 compose 文件，避免不同机器 Docker 环境差异影响默认开发。

## 4. IDEA 环境变量

启用 Milvus 模式时，在 `ScmAiAgentApplication` 的 Environment variables 中配置：

```text
AI_AGENT_RAG_VECTOR_STORE_MODE=milvus
MILVUS_URI=http://localhost:19530
MILVUS_TOKEN=
MILVUS_COLLECTION_NAME=scm_ai_rag_chunks
MILVUS_PRIMARY_FIELD=chunk_id
MILVUS_VECTOR_FIELD=embedding
MILVUS_METRIC_TYPE=COSINE
MILVUS_INDEX_TYPE=AUTOINDEX
```

如果本地 Milvus 没有开启鉴权，`MILVUS_TOKEN` 可以为空。

## 5. Collection Schema 设计

当前 `MilvusRagVectorStore` 会初始化 collection：

```text
collectionName: scm_ai_rag_chunks
primaryField: chunk_id
vectorField: embedding
metricType: COSINE
indexType: AUTOINDEX
```

字段规划：

```text
chunk_id              VarChar primary key
tenant_id           Int64
knowledge_base_id     VarChar
document_id           VarChar
chunk_index           Int32
title                 VarChar
source                VarChar
content               VarChar
embedding             FloatVector
```

说明：

- `tenant_id` 用于租户隔离过滤。
- `knowledge_base_id` 用于知识库范围过滤。
- `document_id`、`chunk_id`、`source`、`title` 用于引用追踪。
- `content` 保存切片内容，便于检索后拼接 RAG 上下文。
- `embedding` 保存向量。

## 6. 检索 Filter 设计

检索时基础过滤条件：

```text
tenant_id == 当前租户 and knowledge_base_id == 当前知识库
```

这保证不同租户、不同知识库之间不会互相召回切片。

## 7. 验证方式

默认模式验证，不需要 Milvus：

```bash
mvn -pl scm-ai-agent -am test
```

真实 Milvus smoke test 的手工验证步骤：

1. 启动 Milvus Standalone。
2. IDEA 启动 `ScmAiAgentApplication`，配置 `AI_AGENT_RAG_VECTOR_STORE_MODE=milvus`。
3. 调用文档写入接口：

```text
POST /api/v1/ai/rag/documents
```

4. 调用检索接口：

```text
POST /api/v1/ai/rag/retrieve
```

5. 调用 RAG Chat：

```text
POST /api/v1/ai/rag/chat
```

## 8. 当前阶段边界

本阶段已经实现 Milvus Adapter 接入点，但不做：

- docs 目录批量导入
- 真实 Embedding API
- MySQL metadata 持久化
- Milvus 运维高可用
- Tools
- MCP
- Workflow
- 多 Agent

下一阶段建议补齐 docs 目录自动导入能力，再接真实 Embedding。