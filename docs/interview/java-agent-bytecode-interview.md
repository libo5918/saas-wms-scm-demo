# Java Agent / 字节码增强面试题

## 1. premain 和 agentmain 有什么区别？

`premain` 是 JVM 启动时通过 `-javaagent` 加载，业务类加载前 Agent 就能注册 transformer，适合生产启动参数部署。

`agentmain` 是 JVM 运行后通过 Attach API 动态加载，适合本地排查、临时诊断和热挂载演示。

当前项目中 `ScmJavaAgent` 同时实现：

```java
public static void premain(String agentArgs, Instrumentation inst)
public static void agentmain(String agentArgs, Instrumentation inst)
```

面试表达：

> premain 偏启动时增强，agentmain 偏运行时 Attach。项目里两个入口都支持，所以既能随服务启动，也能对已运行 JVM 做临时诊断。

## 2. Instrumentation 的作用是什么？

Instrumentation 是 JVM 暴露给 Java Agent 的增强入口，可以：

- 注册 `ClassFileTransformer`。
- 在类加载时修改字节码。
- 对已加载类做 retransform。
- 判断 JVM 是否支持 redefine / retransform。

当前项目使用 Instrumentation 安装 Byte Buddy `AgentBuilder`，并补充 ASM 只读解析能力。

## 3. ASM 和 Byte Buddy 有什么区别？

ASM 更底层，直接面向 class 文件、opcode、method descriptor，控制力强，但开发成本高、可维护性较差。

Byte Buddy 是 ASM 的高层封装，更适合企业级方法插桩、耗时统计、异常监控等场景。

当前项目分工：

- ASM：只读解析 class/method 元信息，展示字节码理解能力。
- Byte Buddy：实现真实方法耗时插桩。

## 4. 当前项目的 Java Agent 做了什么？

`scm-java-agent` 是独立模块，支持：

- `premain`
- `agentmain`
- `Instrumentation`
- Byte Buddy 方法耗时插桩
- ASM 只读 class/method 解析
- Attach 工具
- 慢调用阈值
- 类/方法白名单
- 日志脱敏
- Agent 自我保护

它主要观测 AI Agent 主链路，例如：

- `ToolInvocationService`
- `AgentWorkflowEngine`
- `MultiAgentCoordinatorService`

## 5. 如何控制 Java Agent 的插桩范围？

项目里分两层控制：

1. Byte Buddy type matcher 按类过滤。
2. Advice 内部按方法过滤。

支持参数：

```text
include=com.example.scm.aiagent
exclude=com.example.scm.javaagent
includeClasses=ToolInvocationService|MultiAgentCoordinatorService
excludeClasses=InternalDebugService
includeMethods=invoke|execute|chat
excludeMethods=toString|hashCode
```

这样可以避免全量插桩带来的性能开销和日志噪音。

## 6. slowThresholdMs 是怎么生效的？

`slowThresholdMs` 是慢调用阈值：

- `slowThresholdMs=0`：打印所有匹配方法。
- `slowThresholdMs>0`：只打印耗时大于等于阈值的方法。
- 方法抛异常时，无论是否超过阈值都打印。

这样既能降低日志量，又不会漏掉异常。

## 7. Java Agent 如何避免影响业务稳定性？

核心原则：

- Agent 异常不能影响业务主流程。
- 默认只观测，不修改业务返回值。
- 控制增强范围，只增强白名单包和类。
- 排除 Agent 自身类、JDK 类、Byte Buddy 类和 ASM 类。
- 不打印敏感参数和大对象。
- matcher、transform、Advice、日志输出都做保护性 `try/catch`。

当前项目中 Agent 内部异常只打印脱敏简短错误，不抛给业务。

## 8. 为什么不能打印方法入参和返回值？

企业级 Agent 项目里，入参和返回值可能包含：

- Authorization
- Cookie
- token
- API Key
- prompt
- 模型响应
- rawData
- 业务单据明细

如果 Java Agent 直接打印，会造成敏感信息泄露和日志污染。所以当前项目只打印：

- className
- methodName
- costMs
- success
- slowThresholdMs
- errorType / errorMessage

同时 `AgentLogger.sanitize` 会对敏感字段做 `[REDACTED]` 脱敏。

## 9. Tool Calling、Workflow、Multi-Agent 为什么适合用 Java Agent 观测？

AI Agent 负责智能决策和业务能力调用，但企业开发还需要知道：

- Tool 调用耗时多少？
- Workflow 哪一步慢？
- Multi-Agent 哪个角色失败？
- 异常发生在哪个服务方法？

Java Agent 可以做到不改业务代码就观察这些链路。当前项目推荐观测：

- `ToolInvocationService#invoke`
- `AgentWorkflowEngine#execute`
- `MultiAgentCoordinatorService#chat`

## 10. 如果面试官问“你这个 Agent 和 SkyWalking 有什么区别”怎么答？

可以这样回答：

> SkyWalking 是完整 APM 系统，包含探针、链路追踪、指标采集、存储和 UI。当前项目的 `scm-java-agent` 不是为了替代 SkyWalking，而是为了掌握 Java Agent 的核心开发能力：premain/agentmain、Instrumentation、ASM、Byte Buddy、插桩范围控制、慢调用过滤和脱敏。后续可以把当前采集到的耗时事件对接到 SkyWalking、OpenTelemetry 或 Micrometer。

## 11. 面试项目表达

可以这样讲：

> 我在 Spring AI Agent 项目之外，又补充了一个独立 Java Agent 模块。这个模块不被业务依赖，通过 `-javaagent` 或 Attach 加载。底层基于 Instrumentation 注册 Byte Buddy transformer，用 ASM 做只读字节码解析，用 Byte Buddy 对 AI Agent 主链路做方法耗时和异常观测。JA-2 增加了慢调用阈值、类/方法白名单、敏感日志脱敏和 Agent 自我保护，可以展示企业级 Java Agent 开发中最重要的稳定性和安全边界。
