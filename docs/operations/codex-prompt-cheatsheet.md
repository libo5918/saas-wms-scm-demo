# Codex 提示词速查表

## 1. 最常用万能提示词

```text
请严格按照 docs/architecture/ai-agent-roadmap.md 和 docs/architecture/ai-agent-codex-working-rules.md 推进当前任务。
本次目标：<写清楚你要做的事>
完成后补测试和文档，并告诉我怎么验证。
```

示例：

```text
请严格按照 docs/architecture/ai-agent-roadmap.md 和 docs/architecture/ai-agent-codex-working-rules.md 推进当前任务。
本次目标：实现 scm-ai-agent 的基础 chat 接口。
完成后补测试和文档，并告诉我怎么验证。
```

## 2. 开发新功能

```text
请严格按照 docs/architecture/ai-agent-roadmap.md 和 docs/architecture/ai-agent-codex-working-rules.md 推进当前任务。
本次目标：<功能名>
要求：
1. 先阅读相关代码和 docs，不要凭空设计。
2. 遵守当前模块边界。
3. 涉及接口要给出 Postman 或 curl 验证方式。
4. 涉及架构变化要更新 docs。
5. 完成后跑相关测试。
```

## 3. 开发 AI Agent 能力

```text
请严格按照 docs/architecture/ai-agent-roadmap.md 和 docs/architecture/ai-agent-codex-working-rules.md 推进当前任务。
本次目标：实现 <RAG / Tools / MCP / Workflow / Model Router / Multi-Agent / Trace Metrics>。
要求：
1. 不做空泛 Demo，要结合当前 SCM/WMS 业务。
2. 考虑 gateway、tenantId、userId、roles。
3. 真实调用或包装现有业务服务能力。
4. 考虑工具调用安全、租户隔离和异步任务状态。
5. 完成后补测试和 Markdown 文档。
```

## 4. 写文档

```text
请用 $doc。
请把 <主题> 整理成 docs 下的 Markdown 文档。
要求：
1. 根据内容选择 architecture / business / operations / database 目录。
2. 包含背景、目标、当前实现、流程、接口、测试方式、限制和后续计划。
3. 适合画图的地方用 Mermaid。
```

## 5. 安全审查

```text
请用 $security-best-practices 审查 <模块或功能>。
重点看：
1. JWT / token / 鉴权绕过。
2. 租户隔离和水平越权。
3. Agent tools 是否可能误调用。
4. 写操作是否需要人工确认。
5. API key、模型密钥、MCP 外部访问风险。
```

## 6. 排查问题

```text
当前出现问题：<贴错误信息或现象>。
请按当前项目代码排查：
1. 判断请求是否经过 gateway、auth、业务服务。
2. 检查配置、依赖、过滤器、拦截器、路由和端口。
3. 给出最小验证命令。
4. 如果是代码问题，直接修复并跑相关测试。
```

## 7. 代码审查

```text
请以企业级 Java 后端代码审查视角 review 当前改动。
重点检查：
1. 是否破坏现有业务链路。
2. 是否有租户隔离或越权风险。
3. 是否有异常处理、幂等、重试、事务边界问题。
4. 是否缺少关键测试。
5. 是否符合当前项目模块边界和命名风格。
```

## 8. 面试讲解

```text
请基于当前项目，帮我整理一段 Java AI Agent 开发岗位面试讲解稿。
要求：
1. 区分已完成和规划中。
2. 重点突出 Spring AI、RAG、Tools、MCP、Workflow、Multi-Agent、Trace/Metrics。
3. 结合当前 SCM/WMS 业务场景讲，不要空泛。
4. 给出可能被追问的问题和回答要点。
```

## 9. 创建项目专属 skill

```text
请用 $skill-creator 为当前项目创建一个 scm-ai-agent-dev skill。
要求：
1. 读取 docs/architecture/ai-agent-roadmap.md。
2. 读取 docs/architecture/ai-agent-codex-working-rules.md。
3. 约束后续开发遵守模块边界、测试要求、文档规范和安全规则。
4. 适用于 Spring AI Agent、RAG、Tools、MCP、Workflow、Multi-Agent 开发。
```

## 10. 最少只记三句话

开发：

```text
按 roadmap 和 working-rules 推进，本次目标是 xxx。
```

写文档：

```text
请用 $doc，把 xxx 整理成 docs 下的 Markdown。
```

安全审查：

```text
请用 $security-best-practices，审查 xxx 的安全风险。
```
