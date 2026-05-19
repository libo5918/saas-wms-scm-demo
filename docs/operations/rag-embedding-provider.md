# RAG Embedding Provider 接入与验证说明

## 1. 目标

Phase 3.4 开始把 RAG 的 embedding 从本地 mock 能力扩展到真实模型 Provider。

本阶段目标不是替换整个 RAG 架构，而是在保持默认安全可运行的前提下，让本地可以通过 IDEA 环境变量切换到真实 Embedding：

- 默认：`mock embedding + in-memory`
- 本地 Milvus smoke：`mock embedding + Milvus`
- 真实语义检索 smoke：`DashScope/OpenAI-compatible embedding + Milvus`

## 2. 当前实现

当前项目保留统一抽象：

```text
RagEmbeddingClient
```

默认实现：

```text
MockRagEmbeddingClient
```

真实模型适配器：

```text
SpringAiRagEmbeddingClient
```

当 `ai.agent.rag.embedding.mode=mock` 时，系统只创建 mock embedding，不需要 API Key。

当 `ai.agent.rag.embedding.mode=dashscope` 或 `openai-compatible` 时，系统通过 Spring AI `EmbeddingModel` 调用真实 embedding provider。

## 3. IDEA 环境变量

### 3.1 默认 mock 模式

不配置任何真实 API Key 即可运行：

```text
AI_AGENT_RAG_EMBEDDING_MODE=mock
AI_AGENT_RAG_EMBEDDING_MODEL=mock-embedding
AI_AGENT_RAG_EMBEDDING_DIMENSION=64
AI_AGENT_RAG_VECTOR_STORE_MODE=in-memory
```

### 3.2 DashScope Embedding + Milvus

本地要验证真实语义检索时，建议使用新的 collection 名称，避免和之前 mock 64 维 collection 冲突：

```text
AI_AGENT_RAG_VECTOR_STORE_MODE=milvus
MILVUS_URI=http://localhost:19530
MILVUS_COLLECTION_NAME=scm_ai_rag_chunks_embedding_v3
MILVUS_PRIMARY_FIELD=chunk_id
MILVUS_VECTOR_FIELD=embedding
MILVUS_METRIC_TYPE=COSINE
MILVUS_INDEX_TYPE=AUTOINDEX

AI_AGENT_RAG_EMBEDDING_MODE=dashscope
AI_AGENT_RAG_EMBEDDING_PROVIDER=dashscope
AI_AGENT_RAG_EMBEDDING_MODEL=text-embedding-v3
AI_AGENT_RAG_EMBEDDING_DIMENSION=1024
AI_AGENT_SPRING_AI_MODEL_EMBEDDING=dashscope
AI_AGENT_DASHSCOPE_ENABLED=true
AI_AGENT_DASHSCOPE_EMBEDDING_ENABLED=true
DASHSCOPE_API_KEY=你的本地环境变量
```

注意：不要把真实 `DASHSCOPE_API_KEY` 写入代码、文档或 Git commit。

### 3.3 OpenAI-compatible Embedding 预留

```text
AI_AGENT_RAG_EMBEDDING_MODE=openai-compatible
AI_AGENT_RAG_EMBEDDING_PROVIDER=openai-compatible
AI_AGENT_RAG_EMBEDDING_MODEL=text-embedding-3-small
AI_AGENT_RAG_EMBEDDING_DIMENSION=1536
AI_AGENT_SPRING_AI_MODEL_EMBEDDING=openai
OPENAI_API_KEY=你的本地环境变量
OPENAI_BASE_URL=兼容接口地址
```

## 4. Milvus 维度关系

Milvus collection 的 vector dimension 在创建后不能随意改变。

因此：

- mock embedding 默认 `64` 维。
- DashScope `text-embedding-v3` 本项目建议按 `1024` 维 smoke。
- OpenAI `text-embedding-3-small` 常见配置是 `1536` 维。

如果切换 embedding 模型后继续使用旧 collection，可能会出现写入失败或 schema 不兼容。

处理方式：

1. 本地清空 Milvus 数据：

```bash
docker compose -f deploy/docker-compose/milvus-standalone.yml down -v
docker compose -f deploy/docker-compose/milvus-standalone.yml up -d
```

2. 或者换新 collection：

```text
MILVUS_COLLECTION_NAME=scm_ai_rag_chunks_embedding_v3
```

## 5. 通过 gateway 18080 验证

### 5.1 导入 docs

```http
POST http://localhost:18080/api/v1/ai/rag/import/docs
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{
  "knowledgeBaseId": "kb-project-docs",
  "scanRoot": "docs",
  "includeDirectories": ["operations"],
  "supportedExtensions": [".md"],
  "maxFiles": 100
}
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "knowledgeBaseId": "kb-project-docs",
    "vectorStoreMode": "milvus",
    "embeddingMode": "dashscope",
    "importedCount": 1
  }
}
```

### 5.2 检索 SkyWalking

```http
POST http://localhost:18080/api/v1/ai/rag/retrieve
Authorization: Bearer <access_token>
Content-Type: application/json
```

```json
{
  "knowledgeBaseId": "kb-project-docs",
  "query": "SkyWalking 接入说明",
  "topK": 5,
  "filters": {
    "directory": "docs/operations",
    "importSource": "docs-auto-import"
  }
}
```

关键预期返回：

```json
{
  "success": true,
  "data": {
    "knowledgeBaseId": "kb-project-docs",
    "retrievedCount": 5,
    "chunks": [
      {
        "source": "docs/operations/auth-gateway-nacos-skywalking-quickstart.md"
      }
    ]
  }
}
```

真实 embedding 接入后，`SkyWalking` 相关文档应该比 mock embedding 更稳定地排到前面。

## 6. 当前边界

本阶段不实现：

- 复杂 rerank
- Tools
- MCP
- Workflow
- 多 Agent
- 长任务编排
- MySQL RAG metadata 持久化

## 7. Phase 3.5：检索质量增强配置

Phase 3.5 在真实 Embedding 可用的基础上，新增两个检索质量控制点。

第一，`scoreThreshold` 用于过滤低分 chunk：

```yaml
ai.agent.rag.retrieval.score-threshold: 0
```

接口也可以单次覆盖：

```json
{
  "knowledgeBaseId": "kb-project-docs",
  "query": "SkyWalking 接入说明",
  "topK": 5,
  "scoreThreshold": 0.2
}
```

第二，RAG Chat 上下文裁剪用于控制 prompt 长度：

```yaml
ai.agent.rag.retrieval.max-context-chunks: 5
ai.agent.rag.retrieval.max-context-chunk-length: 1200
```

注意：

- `scoreThreshold` 设置越高，结果越严格，但也更容易无结果。
- mock embedding 的分数不代表真实语义质量，真实判断应优先使用 DashScope/OpenAI-compatible embedding 验证。
- 当前阶段只预留 rerank 扩展点，不调用 rerank 模型。