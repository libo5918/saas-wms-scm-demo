# Java Agent 运行手册

## 打包与测试

在项目根目录执行：

```bash
mvn -pl scm-java-agent -am test
```

如果需要重新生成 Agent Jar：

```bash
mvn -pl scm-java-agent -am package -DskipTests
```

产物：

```text
scm-java-agent/target/scm-java-agent-1.0.0-SNAPSHOT.jar
```

该 Jar 通过 shade 插件包含 Byte Buddy 和 ASM 依赖，可直接作为 `-javaagent` 使用。

## premain 启动加载

Linux / macOS 示例：

```bash
java \
  -javaagent:scm-java-agent/target/scm-java-agent-1.0.0-SNAPSHOT.jar=include=com.example.scm.aiagent,slowThresholdMs=200,includeClasses=ToolInvocationService|MultiAgentCoordinatorService,includeMethods=invoke|chat \
  -jar scm-ai-agent/target/scm-ai-agent-1.0.0-SNAPSHOT.jar
```

Windows PowerShell 中建议把 `-javaagent` 参数整体加引号，避免 `|` 被 Shell 解释：

```powershell
java "-javaagent:scm-java-agent\target\scm-java-agent-1.0.0-SNAPSHOT.jar=include=com.example.scm.aiagent,slowThresholdMs=200,includeClasses=ToolInvocationService|MultiAgentCoordinatorService,includeMethods=invoke|chat" -jar scm-ai-agent\target\scm-ai-agent-1.0.0-SNAPSHOT.jar
```

如果业务 Jar 不存在，先执行：

```bash
mvn -pl scm-ai-agent -am package -DskipTests
```

预期日志：

```text
[SCM-JAVA-AGENT] SCM Java Agent loaded, loadMode=premain
[SCM-JAVA-AGENT] Byte Buddy instrumentation installed
```

调用 AI Agent 接口后，可看到方法耗时日志：

```text
[SCM-JAVA-AGENT] method=com.example.scm.aiagent.tool.service.ToolInvocationService#invoke, costMs=35, success=true, slowThresholdMs=200
```

## agentmain 运行时 Attach

先正常启动业务服务，然后获取 PID：

```bash
jps -l
```

Attach 示例：

```bash
java --add-modules jdk.attach \
  -cp scm-java-agent/target/scm-java-agent-1.0.0-SNAPSHOT.jar \
  com.example.scm.javaagent.attach.AttachAgentLauncher \
  <pid> \
  scm-java-agent/target/scm-java-agent-1.0.0-SNAPSHOT.jar \
  "include=com.example.scm.aiagent,slowThresholdMs=200,includeClasses=ToolInvocationService|AgentWorkflowEngine,includeMethods=invoke|execute"
```

Windows PowerShell 示例：

```powershell
java --add-modules jdk.attach -cp scm-java-agent\target\scm-java-agent-1.0.0-SNAPSHOT.jar com.example.scm.javaagent.attach.AttachAgentLauncher <pid> scm-java-agent\target\scm-java-agent-1.0.0-SNAPSHOT.jar "include=com.example.scm.aiagent,slowThresholdMs=200,includeClasses=ToolInvocationService|AgentWorkflowEngine,includeMethods=invoke|execute"
```

预期日志：

```text
SCM Java Agent attached, pid=<pid>
[SCM-JAVA-AGENT] SCM Java Agent loaded, loadMode=agentmain
```

## 常用参数

| 参数 | 示例 | 说明 |
| --- | --- | --- |
| `enabled` | `enabled=true` | 是否启用 Agent |
| `include` | `include=com.example.scm.aiagent` | 包名前缀白名单 |
| `exclude` | `exclude=com.example.scm.javaagent` | 包名前缀黑名单 |
| `includeClasses` | `includeClasses=ToolInvocationService|AgentWorkflowEngine` | 类名白名单 |
| `excludeClasses` | `excludeClasses=InternalDebugService` | 类名黑名单 |
| `includeMethods` | `includeMethods=invoke|execute|chat` | 方法名白名单 |
| `excludeMethods` | `excludeMethods=toString|hashCode` | 方法名黑名单 |
| `traceTool` | `traceTool=true` | 是否观测 Tool 链路 |
| `traceWorkflow` | `traceWorkflow=true` | 是否观测 Workflow 链路 |
| `traceMultiAgent` | `traceMultiAgent=true` | 是否观测 Multi-Agent 链路 |
| `asmPrint` | `asmPrint=false` | 是否打印 ASM 只读解析结果 |
| `slowThresholdMs` | `slowThresholdMs=200` | 慢调用阈值，0 表示全部打印 |

## 慢调用阈值

```text
slowThresholdMs=0
```

表示所有匹配方法都打印。

```text
slowThresholdMs=200
```

表示只打印耗时大于等于 200ms 的匹配方法。异常方法无论耗时多少都会打印，便于定位失败。

## 白名单示例

只观测 Tool 调用入口和 Multi-Agent 协调入口：

```text
include=com.example.scm.aiagent,includeClasses=ToolInvocationService|MultiAgentCoordinatorService,includeMethods=invoke|chat,slowThresholdMs=100
```

排除基础方法：

```text
excludeMethods=toString|hashCode|equals
```

## ASM 元信息打印

开启：

```text
asmPrint=true
```

预期输出：

```text
[SCM-JAVA-AGENT] [ASM] class=com.example.scm.aiagent.tool.service.ToolInvocationService
[SCM-JAVA-AGENT] [ASM] method=com.example.scm.aiagent.tool.service.ToolInvocationService#invoke
```

## 安全注意事项

Java Agent 日志不得输出：

- Authorization
- Cookie
- token / accessToken / refreshToken
- API Key / apiKey / api-key
- password / secret
- 完整 prompt
- 完整模型响应
- 完整 rawData
- 大段业务入参或返回对象

当前版本只输出类名、方法名、耗时、成功状态、慢调用阈值和异常概要。Agent 内部异常会被保护性捕获，不应影响业务主流程。

## 常见问题

### Unable to access jarfile

如果启动时报：

```text
Error: Unable to access jarfile scm-ai-agent/target/scm-ai-agent-1.0.0-SNAPSHOT.jar
```

说明业务 Jar 还没打包，先执行：

```bash
mvn -pl scm-ai-agent -am package -DskipTests
```

### PowerShell 中 `|` 参数异常

PowerShell 会把 `|` 当管道符，建议把整个 `-javaagent` 参数用双引号包起来。

## 面试讲解口径

可以这样讲：

> 我在项目中单独做了一个 `scm-java-agent` 模块，支持 premain 和 agentmain 两种加载方式。底层通过 Instrumentation 注册 Byte Buddy 增强逻辑，用 ASM 做只读 class/method 元信息解析。JA-2 增加了慢调用阈值、类/方法白名单、日志脱敏和 Agent 自我保护，能在不改业务代码、不让业务模块依赖 Agent 的情况下观测 Tool、Workflow、Multi-Agent 主链路耗时和异常。
