# Java Agent 运行手册

## 打包

在项目根目录执行：

```bash
mvn -pl scm-java-agent -am package
```

产物：

```text
scm-java-agent/target/scm-java-agent-1.0.0-SNAPSHOT.jar
```

该 jar 通过 shade 插件包含 Byte Buddy 和 ASM 依赖，可作为 `-javaagent` 使用。

## premain 启动加载

示例：

```bash
java ^
  -javaagent:scm-java-agent/target/scm-java-agent-1.0.0-SNAPSHOT.jar=include=com.example.scm.aiagent,traceTool=true,traceWorkflow=true,traceMultiAgent=true ^
  -jar scm-ai-agent/target/scm-ai-agent-1.0.0-SNAPSHOT.jar
```

预期日志：

```text
[SCM-JAVA-AGENT] SCM Java Agent loaded, loadMode=premain
[SCM-JAVA-AGENT] Byte Buddy instrumentation installed
```

调用 AI Agent 接口后，可观察方法耗时：

```text
[SCM-JAVA-AGENT] method=com.example.scm.aiagent.tool.service.ToolInvocationService#invoke, costMs=35, success=true
```

## agentmain 运行时热挂载

先正常启动业务服务，然后获取 PID。

Windows 示例：

```bash
jps -l
```

Attach：

```bash
java --add-modules jdk.attach ^
  -cp scm-java-agent/target/scm-java-agent-1.0.0-SNAPSHOT.jar ^
  com.example.scm.javaagent.attach.AttachAgentLauncher ^
  <pid> ^
  scm-java-agent/target/scm-java-agent-1.0.0-SNAPSHOT.jar ^
  include=com.example.scm.aiagent,traceTool=true,traceWorkflow=true,traceMultiAgent=true
```

预期日志：

```text
SCM Java Agent attached, pid=<pid>
[SCM-JAVA-AGENT] SCM Java Agent loaded, loadMode=agentmain
```

## 常用参数

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

说明：

- `enabled`：是否启用 Agent。
- `include`：增强包名前缀。
- `exclude`：排除包名前缀。
- `traceTool`：是否跟踪 Tool 调用链路。
- `traceWorkflow`：是否跟踪 Workflow 链路。
- `traceMultiAgent`：是否跟踪 Multi-Agent 链路。
- `asmPrint`：是否打印 ASM 解析到的 class / method 元信息。
- `slowThresholdMs`：慢调用阈值，当前已解析，后续可升级为过滤条件。

## ASM 元信息打印

开启：

```bash
-javaagent:scm-java-agent.jar=include=com.example.scm.aiagent,asmPrint=true
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
- token
- API Key
- 完整 prompt
- 完整模型响应
- 完整 rawData
- 大段业务入参或返回对象

当前版本只输出类名、方法名、耗时、成功状态和异常类型，避免污染业务日志和泄露敏感信息。

## 面试讲解口径

可以这样描述：

> 我在项目中新增了一个独立 Java Agent 模块，支持 premain 启动加载和 agentmain 运行时 attach。底层基于 Instrumentation 注册增强逻辑，使用 ASM 做 class/method 元信息解析，用 Byte Buddy 对 AI Agent 主链路做方法耗时和异常观测。插桩点覆盖 ToolInvocationService、Workflow Engine、Multi-Agent Coordinator 等核心链路，做到不修改业务代码即可观察 Agent 执行耗时和失败情况。
