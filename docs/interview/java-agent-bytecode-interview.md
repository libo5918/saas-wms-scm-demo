# Java Agent / 字节码增强面试题

## premain 和 agentmain 有什么区别？

`premain` 是 JVM 启动时通过 `-javaagent` 加载，业务类加载前 Agent 就能注册 transformer，适合生产启动参数部署。

`agentmain` 是 JVM 运行后通过 Attach API 动态加载，适合线上排查、本地演示或临时诊断。

当前项目中 `ScmJavaAgent` 同时实现：

```java
public static void premain(String agentArgs, Instrumentation inst)
public static void agentmain(String agentArgs, Instrumentation inst)
```

## Instrumentation 的作用是什么？

Instrumentation 是 JVM 暴露给 Java Agent 的增强入口，可以：

- 注册 `ClassFileTransformer`。
- 在类加载时修改字节码。
- 对已加载类做 retransform。
- 判断是否支持 redefine / retransform。

当前项目使用 Instrumentation 安装 Byte Buddy AgentBuilder，并补充 ASM 只读解析能力。

## ASM 和 Byte Buddy 有什么区别？

ASM 更底层，直接面对 class 文件、opcode、method descriptor，控制力强，但开发成本高、可维护性差。

Byte Buddy 是 ASM 的高层封装，更适合企业级方法插桩、耗时统计、异常监控等场景。

当前项目中的分工是：

- ASM：读取 class/method 元信息，辅助理解字节码结构。
- Byte Buddy：实现真实方法耗时插桩。

## Java Agent 如何避免影响业务稳定性？

核心原则：

- Agent 异常不能影响业务主流程。
- 默认只做观测，不修改返回值。
- 控制增强范围，只增强白名单包。
- 排除 Agent 自身类、JDK 类、Byte Buddy 类和 ASM 类。
- 不打印敏感参数和大对象。
- 控制日志量，后续可增加慢调用阈值。

当前项目默认只记录：

- className
- methodName
- costMs
- success
- errorType

## 为什么适合和当前 AI Agent 项目结合？

AI Agent 负责智能决策和业务能力调用，但企业开发中还需要知道：

- Tool 调用耗时多少？
- Workflow 哪一步慢？
- Multi-Agent 哪个角色失败？
- 异常发生在哪个服务方法？

Java Agent 可以做到不改业务代码就观测这些链路。

当前推荐插桩点：

- `ToolInvocationService`
- `AgentWorkflowEngine`
- `MultiAgentCoordinatorService`

## 面试项目表达

可以这样讲：

> 我在项目中除了实现 Spring AI 方向的 AI Agent，也补充了 JVM Java Agent 能力。Java Agent 模块支持 premain 和 agentmain 两种加载方式，基于 Instrumentation 注册增强逻辑。底层用 ASM 做 class/method 元信息解析，高层用 Byte Buddy 实现方法耗时、异常监控和慢调用日志。插桩点覆盖 ToolInvocationService、Workflow Engine、Multi-Agent Coordinator 等 AI Agent 核心链路，不修改业务代码即可观察 Agent 执行耗时、失败原因和调用路径。
