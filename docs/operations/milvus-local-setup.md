# Milvus 本地搭建与验证说明

## 1. Milvus 是什么

Milvus 是一个开源向量数据库，常用于 RAG、相似图片检索、推荐系统和语义搜索。

在当前 `saas-wms-scm` 项目里，Milvus 的角色是：

```text
docs Markdown 文档
 -> 文档切片 chunk
 -> Embedding 向量
 -> 写入 Milvus collection
 -> 按 query 向量 topK 检索
 -> 返回 citations
 -> 拼接 RAG prompt
 -> 交给模型生成回答
```

注意：Milvus 不负责“理解文本”。真正把文本变成向量的是 Embedding 模型。当前阶段仍使用 mock embedding，只用于验证 Milvus 写入、检索和 metadata filter 链路。

## 2. 核心概念

### 2.1 Collection

Collection 类似关系型数据库里的表，用来保存同一类向量数据。

当前项目默认 collection：

```text
scm_ai_rag_chunks
```

### 2.2 Schema

Schema 定义 collection 里有哪些字段。当前项目以 RAG chunk 为最小检索单元。

当前字段规划：

```text
chunk_id              VarChar primary key
 tenant_id             Int64
knowledge_base_id     VarChar
document_id           VarChar
chunk_index           Int32
title                 VarChar
source                VarChar
file_path             VarChar
file_name             VarChar
directory             VarChar
extension             VarChar
import_source         VarChar
content               VarChar
embedding             FloatVector
```

### 2.3 Field

Field 是字段，例如 `tenant_id`、`knowledge_base_id`、`content`。

### 2.4 Vector Field

Vector field 是向量字段，当前项目是：

```text
embedding
```

它保存每个 chunk 的向量。

### 2.5 Scalar Field

Scalar field 是普通字段，例如：

```text
tenant_id
knowledge_base_id
document_id
source
directory
import_source
```

它们用于过滤、租户隔离、知识库隔离和引用追踪。

### 2.6 Index

Index 是向量索引，用于提升向量检索性能。当前本地 smoke 使用：

```text
AUTOINDEX
```

### 2.7 Metric Type

Metric type 是相似度计算方式。当前项目默认：

```text
COSINE
```

意思是按余弦相似度检索向量。

### 2.8 topK

topK 表示返回最相似的前 N 条 chunk。

例如 `topK=3`，表示最多返回 3 个最相关切片。

### 2.9 Filter Expression

Filter expression 是 Milvus 的标量过滤表达式。

当前项目每次检索都会强制拼接：

```text
tenant_id == 当前租户 and knowledge_base_id == 当前知识库
```

如果接口传入 metadata filter，会继续拼接白名单字段，例如：

```text
directory == "docs/operations"
file_path == "docs/operations/skywalking-integration.md"
import_source == "docs-auto-import"
```

## 3. Docker Desktop 前提

本地需要先安装 Docker Desktop，并确保 Docker 可以运行 Linux containers。

检查命令：

```bash
docker version
docker compose version
```

如果命令不可用，先安装并启动 Docker Desktop。

## 4. 启动 Milvus Standalone

项目已提供本地开发用 compose 文件：

```text
deploy/docker-compose/milvus-standalone.yml
```

启动命令：

```bash
docker compose -f deploy/docker-compose/milvus-standalone.yml up -d
```

查看容器：

```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

正常应看到：

```text
scm-milvus-etcd
scm-milvus-minio
scm-milvus-standalone
scm-milvus-attu
```

停止命令：

```bash
docker compose -f deploy/docker-compose/milvus-standalone.yml down
```

如果要清空本地 Milvus 数据：

```bash
docker compose -f deploy/docker-compose/milvus-standalone.yml down -v
```

## 5. 默认端口

当前 compose 端口规划：

```text
19530  Milvus gRPC / Java SDK 连接端口
19091  Milvus HTTP health / metrics 端口，映射容器 9091
19000  MinIO API，映射容器 9000
19001  MinIO Console，映射容器 9001
18000  Attu Web UI，映射容器 3000
```

当前项目连接 Milvus 使用：

```text
MILVUS_URI=http://localhost:19530
```

## 6. 确认 Milvus 启动成功

查看 standalone 日志：

```bash
docker logs scm-milvus-standalone --tail 100
```

查看 health：

```bash
curl http://localhost:19091/healthz
```

如果返回健康状态，说明 Milvus standalone 已经可以被 Java SDK 连接。

## 6.1 使用 Attu 查看 Milvus 数据

Attu 是 Milvus 常用的 Web UI 客户端，作用类似 MySQL 场景里的 Navicat / DataGrip，只是它面向 Milvus collection、schema、向量数据和检索结果。

当前 compose 已经包含 Attu：

```text
scm-milvus-attu
```

启动 compose 后，在浏览器打开：

```text
http://localhost:18000
```

如果 Attu 页面要求填写 Milvus 连接地址，可以使用：

```text
milvus-standalone:19530
```

如果你在宿主机上的其它客户端里连接 Milvus，则使用：

```text
localhost:19530
```

在 Attu 中你可以重点观察：

- `scm_ai_rag_chunks` collection 是否创建成功
- schema 字段是否包含 `tenant_id`、`knowledge_base_id`、`file_path`、`directory`、`embedding`
- docs 导入后是否有 chunk 数据
- metadata filter 检索后返回的字段是否符合预期

## 7. IDEA 环境变量配置

默认项目仍然使用 in-memory，不连接 Milvus。

要切换到 Milvus，在 IDEA 的 `ScmAiAgentApplication` Run/Debug Configuration 里配置 Environment variables：

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

如果本地 Milvus 没有开启鉴权，`MILVUS_TOKEN` 留空即可。

为了本地绕开 Nacos，也可以在 Program arguments 中加：

```text
--spring.cloud.nacos.discovery.enabled=false
--spring.cloud.nacos.config.enabled=false
```

## 8. 通过当前项目写入 docs 文档到 Milvus

启动顺序建议：

1. 启动 Milvus Standalone。
2. 启动 `scm-auth`。
3. 启动 `scm-ai-agent`，并配置 `AI_AGENT_RAG_VECTOR_STORE_MODE=milvus`。
4. 启动 `scm-gateway`。
5. 登录获取 token。
6. 通过 gateway 调用 docs 导入接口。

接口：

```http
POST http://localhost:18080/api/v1/ai/rag/import/docs
Authorization: Bearer <access_token>
Content-Type: application/json
```

请求体：

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
    "embeddingMode": "mock",
    "importedCount": 1
  }
}
```

## 9. 通过当前项目检索 Milvus 文档

接口：

```http
POST http://localhost:18080/api/v1/ai/rag/retrieve
Authorization: Bearer <access_token>
Content-Type: application/json
```

请求体：

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
    "retrievedCount": 1,
    "chunks": [
      {
        "source": "docs/operations/skywalking-integration.md",
        "metadata": {
          "directory": "docs/operations",
          "importSource": "docs-auto-import"
        }
      }
    ]
  }
}
```

说明：当前仍是 mock embedding，语义排序不一定精准；本阶段重点验证 Milvus 写入、检索、metadata filter 和 citations 链路。

## 10. RAG Chat 验证

接口：

```http
POST http://localhost:18080/api/v1/ai/rag/chat
Authorization: Bearer <access_token>
Content-Type: application/json
```

请求体：

```json
{
  "knowledgeBaseId": "kb-project-docs",
  "message": "SkyWalking 接入说明？",
  "taskType": "rag_qa",
  "providerMode": "mock",
  "requestedModel": "qwen-plus",
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
    "chat": {
      "providerMode": "mock",
      "taskType": "rag_qa"
    },
    "citations": [
      {
        "source": "docs/operations/skywalking-integration.md"
      }
    ]
  }
}
```

## 11. 常见问题

### 11.1 启动 scm-ai-agent 时报 collection schema incompatible

原因：你本地已经存在旧 schema 的 `scm_ai_rag_chunks` collection。

处理方式二选一：

1. 清空 Milvus 本地数据：

```bash
docker compose -f deploy/docker-compose/milvus-standalone.yml down -v
```

2. 或者换一个 collection 名称：

```text
MILVUS_COLLECTION_NAME=scm_ai_rag_chunks_v2
```

### 11.2 检索不到 SkyWalking 文档

先确认是否导入了 `docs/operations`：

```json
{
  "includeDirectories": ["operations"],
  "maxFiles": 100
}
```

然后检索时加 metadata filter：

```json
{
  "filters": {
    "directory": "docs/operations"
  }
}
```

### 11.3 返回答案仍然不像真实问答

如果 `providerMode=mock`，返回的是 mock 调试响应，不是真实模型生成。

要真实总结，需要后续切换：

```text
providerMode=spring-ai
```

并配置真实 Qwen / OpenAI-compatible API Key。

### 11.4 检索排序仍然不准

当前仍使用 mock embedding，Milvus 只能按 mock 向量检索。

下一阶段接入真实 Embedding 后，语义检索质量才会明显提升。

## 12. 当前阶段边界

Phase 3.3 只做：

- Milvus Docker 本地搭建
- Milvus collection 初始化
- Milvus schema 检查
- Milvus upsert/search smoke 验证
- metadata filter 最小实现
- 默认测试不连接 Milvus

本阶段不做：

- 真实 Embedding API
- MySQL RAG metadata 持久化
- Milvus 高可用运维
- Tools
- MCP
- Workflow
- 多 Agent
- 长任务编排

## 13. Milvus 新手学习路线

建议按当前项目顺序学习：

1. 先理解 RAG 链路：文档、chunk、embedding、vector store、topK、citations。
2. 用 `in-memory` 跑通导入和检索，理解接口输入输出。
3. 启动 Milvus Standalone，理解 collection/schema/vector field/scalar field。
4. 用当前项目把 docs 文档写入 Milvus。
5. 用 `filters.directory=docs/operations` 理解 metadata filter。
6. 用 `tenant_id + knowledge_base_id` 理解企业级多租户隔离。
7. 后续接真实 Embedding，观察 mock embedding 和真实 embedding 的检索差异。
8. 再学习 index type、metric type、分区、批量导入、备份和分布式部署。
