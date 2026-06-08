# Java Agent 字节码增强设计

## 目标

`scm-java-agent` 是当前项目独立的 JVM Java Agent 模块，用来补充企业级 Agent 项目中常见的无侵入观测能力。它和 `scm-ai-agent` 里的大模型 Agent 不是同一个概念：

- AI Agent：负责 RAG、Tool Calling、Workflow、MCP、Multi-Agent 等智能应用能力。
- Java Agent：负责 JVM 层无侵入插桩、方法耗时观测、异常观测和运行时 Attach。

本模块不要求业务模块依赖它，业务服务可以通过 `-javaagent` 启动加载，也可以通过 Attach API 在运行时动态加载。

## 模块结构

```text
scm-java-agent
├── ScmJavaAgent.java
├── attach
│   └── AttachAgentLauncher.java
├── asm
│   └── AsmClassPrinter.java
├── bytebuddy
│   ├── ByteBuddyAgentInstaller.java
│   ├── MethodTimingAdvice.java
│   └── MethodTraceDecision.java
├── config
│   ├── AgentConfig.java
│   └── AgentRuntimeConfig.java
└── logging
    └── AgentLogger.java
```

## premain 与 agentmain

`ScmJavaAgent` 同时提供两个入口：

```java
public static void premain(String agentArgs, Instrumentation inst)
public static void agentmain(String agentArgs, Instrumentation inst)
```

- `premain`：JVM 启动时通过 `-javaagent` 加载，适合本地演示和生产启动参数部署。
- `agentmain`：JVM 运行后通过 Attach API 动态加载，适合本地排查、临时诊断和热挂载演示。

MANIFEST 中声明：

```text
Premain-Class: com.example.scm.javaagent.ScmJavaAgent
Agent-Class: com.example.scm.javaagent.ScmJavaAgent
Can-Redefine-Classes: true
Can-Retransform-Classes: true
```

## Byte Buddy 插桩策略

当前真实方法增强使用 Byte Buddy 完成。默认观测范围是：

- `com.example.scm.aiagent` 包下类。
- Controller、Service、Tool、Workflow、Multi-Agent 相关类。
- 重点观测 `ToolInvocationService`、`AgentWorkflowEngine`、`MultiAgentCoordinatorService` 等 AI Agent 主链路。

默认排除：

- `com.example.scm.javaagent`
- `net.bytebuddy`
- `org.objectweb.asm`
- `java` / `jdk` / `sun`

方法进入时记录开始时间，方法退出时记录耗时、成功状态、慢调用阈值和异常概要。Agent 不打印入参、返回值、完整 prompt、完整模型响应或 rawData。

## ASM 的定位

ASM 在本项目中先作为只读字节码解析能力展示：

- 读取 className。
- 读取 methodName。
- 读取 method descriptor。

不直接用 ASM 修改业务方法体，原因是企业项目中 Byte Buddy 更适合快速、稳定地完成方法级插桩；ASM 更适合做底层 class 文件结构理解，或在需要极细粒度字节码控制时使用。

## JA-2 配置增强

Agent 参数通过 `-javaagent:path=key=value,key2=value2` 传入。JA-2 增加了更可控的插桩范围和慢调用过滤。

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用 Agent |
| `include` | `com.example.scm.aiagent` | 包名前缀白名单 |
| `exclude` | `com.example.scm.javaagent` | 包名前缀黑名单 |
| `includeClasses` | 空 | 类名白名单，支持 `|` 或 `,` 分隔 |
| `excludeClasses` | 空 | 类名黑名单，支持 `|` 或 `,` 分隔 |
| `includeMethods` | 空 | 方法名白名单，支持 `|` 或 `,` 分隔 |
| `excludeMethods` | 空 | 方法名黑名单，支持 `|` 或 `,` 分隔 |
| `traceTool` | `true` | 是否观测 Tool 链路 |
| `traceWorkflow` | `true` | 是否观测 Workflow 链路 |
| `traceMultiAgent` | `true` | 是否观测 Multi-Agent 链路 |
| `asmPrint` | `false` | 是否打印 ASM 只读解析信息 |
| `slowThresholdMs` | `0` | 慢调用阈值，0 表示全部打印 |

列表参数推荐使用 `|`：

```text
includeClasses=ToolInvocationService|MultiAgentCoordinatorService
includeMethods=invoke|execute|chat
excludeMethods=toString|hashCode
```

## 慢调用阈值设计

`slowThresholdMs` 的行为：

- `slowThresholdMs=0`：打印所有匹配方法调用。
- `slowThresholdMs>0`：只打印耗时大于等于阈值的方法。
- 方法抛异常时，无论是否超过阈值都打印，便于定位失败。

日志字段：

- `className`
- `methodName`
- `costMs`
- `success`
- `slowThresholdMs`
- `errorType`
- `errorMessage`

不打印入参和返回值，避免污染业务日志和泄露敏感数据。

## 类与方法过滤

过滤分两层：

1. Byte Buddy type matcher 先按类过滤，避免不必要的插桩。
2. `MethodTimingAdvice` 内部再按方法名过滤，避免打印无意义方法。

规则：

- `include` / `exclude` 按完整类名包前缀匹配。
- `includeClasses` / `excludeClasses` 支持简单类名或完整类名。
- `includeMethods` / `excludeMethods` 按方法名匹配。
- `exclude` 优先于 `include`。
- `excludeMethods` 优先于 `includeMethods`。

## 日志脱敏

`AgentLogger.sanitize` 会对以下敏感字段做脱敏：

- `authorization`
- `cookie`
- `token`
- `accessToken`
- `refreshToken`
- `apiKey`
- `api-key`
- `password`
- `secret`
- `rawData`
- `prompt`
- `model response`

脱敏后统一替换为 `[REDACTED]`。Java Agent 仍使用轻量 `System.out/System.err`，不引入业务日志框架，避免和业务日志体系耦合。

## 自我保护

Java Agent 必须遵守稳定性边界：

- Agent 内部异常不能影响业务主流程。
- matcher、transform、Advice、日志输出都需要保护性 `try/catch`。
- Agent 失败时只打印简短脱敏错误。
- 默认只观测，不修改业务返回值。
- 不让业务模块依赖 `scm-java-agent`。

## 与 AI Agent 主链路的结合

推荐演示插桩点：

- `ToolInvocationService`：观察 Tool 调用耗时、失败和慢调用。
- `AgentWorkflowEngine`：观察 Workflow 步骤执行耗时。
- `MultiAgentCoordinatorService`：观察 Multi-Agent 协作入口耗时。

面试表达可以这样说：

> AI Agent 负责智能规划、RAG、工具调用和多 Agent 协作；Java Agent 负责 JVM 层无侵入观测。项目中我通过 premain/agentmain、Instrumentation、Byte Buddy 和 ASM，把 AI Agent 主链路的方法耗时和异常观测做成独立 Agent 模块，不要求业务模块依赖它。

## 后续扩展

JA-3 可继续推进：

- traceId / runId 透传与日志关联。
- 方法调用采样率。
- Agent 观测事件异步缓冲。
- 输出到文件或 Micrometer。
- 与 SkyWalking / OpenTelemetry 对接。
- 更细粒度类重转换与动态配置刷新。
