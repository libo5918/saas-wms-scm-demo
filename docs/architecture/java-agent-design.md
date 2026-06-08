# Java Agent 字节码增强设计

## 目标

`scm-java-agent` 是当前项目新增的 JVM Java Agent 模块，用于补充 `premain`、`agentmain`、Instrumentation、ASM、Byte Buddy 等字节码增强能力。

它和 `scm-ai-agent` 中的大模型 AI Agent 不是同一个概念：

- AI Agent：负责 RAG、Tool Calling、Workflow、MCP、Multi-Agent 等智能应用能力。
- Java Agent：负责 JVM 层无侵入插桩、方法耗时监控、异常观测和运行时 attach。

本模块的定位是面试级企业实践闭环：不修改业务代码，即可观察 AI Agent 主链路的执行耗时和异常。

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
│   └── MethodTimingAdvice.java
├── config
│   └── AgentConfig.java
└── logging
    └── AgentLogger.java
```

## premain 与 agentmain

`ScmJavaAgent` 同时提供两个入口：

```java
public static void premain(String agentArgs, Instrumentation inst)
public static void agentmain(String agentArgs, Instrumentation inst)
```

- `premain`：JVM 启动时通过 `-javaagent` 加载，适合生产启动参数部署。
- `agentmain`：服务运行后通过 Attach API 动态加载，适合本地排查和演示热挂载。

MANIFEST 中声明：

```text
Premain-Class: com.example.scm.javaagent.ScmJavaAgent
Agent-Class: com.example.scm.javaagent.ScmJavaAgent
Can-Redefine-Classes: true
Can-Retransform-Classes: true
```

## Byte Buddy 插桩策略

当前默认增强范围：

- `com.example.scm.aiagent` 包下类。
- 重点覆盖 Tool、Workflow、Multi-Agent 相关类。
- 也会覆盖 `Controller`、`Service` 后缀类，便于观察主链路。

默认排除：

- `com.example.scm.javaagent`
- `net.bytebuddy`
- `org.objectweb.asm`
- `java`
- `jdk`
- `sun`

插桩逻辑使用 `MethodTimingAdvice`：

- 方法进入时记录 `System.nanoTime()`。
- 方法退出时输出耗时、成功状态和异常类型。
- 不打印入参、返回值、完整 prompt、完整模型响应或 rawData。

示例日志：

```text
[SCM-JAVA-AGENT] method=com.example.scm.aiagent.tool.service.ToolInvocationService#invoke, costMs=35, success=true
```

## ASM 的定位

本项目中 ASM 先做只读能力展示：

- 读取 className。
- 读取 methodName。
- 读取 method descriptor。

不在第一版直接用 ASM 修改方法体，因为企业实践中 Byte Buddy 更适合快速、安全地完成方法插桩。ASM 更适合理解底层 class 文件结构，或在需要极细粒度字节码控制时使用。

## 配置参数

Agent 参数通过 `-javaagent:path=key=value,key2=value2` 传入：

```text
enabled=true
include=com.example.scm.aiagent
exclude=com.example.scm.javaagent
traceTool=true
traceWorkflow=true
traceMultiAgent=true
asmPrint=false
slowThresholdMs=200
```

当前 `slowThresholdMs` 已完成配置解析和日志输出，后续可继续升级为慢调用过滤。

## 安全边界

Java Agent 必须遵守以下边界：

- 不打印 Authorization、Cookie、token、API Key。
- 不打印完整 prompt。
- 不打印完整模型响应。
- 不打印完整 rawData。
- Agent 自身异常不能影响业务主流程。
- 默认只做观测，不改变业务返回值。

## 与 AI Agent 主链路的结合

推荐演示插桩点：

- `ToolInvocationService`：观察工具调用耗时。
- `AgentWorkflowEngine`：观察 Workflow 执行耗时。
- `MultiAgentCoordinatorService`：观察 Multi-Agent 协作耗时。

这样面试时可以形成清晰表达：

> AI Agent 负责智能规划、工具调用和总结；Java Agent 负责无侵入式观测和字节码增强。

## 后续扩展

可继续推进：

- 慢调用阈值过滤。
- traceId / runId 透传。
- 参数摘要脱敏采样。
- 方法白名单配置。
- MySQL / 日志文件输出。
- 与 SkyWalking / Micrometer 对接。
- 对指定类做 retransformation。
