package com.example.scm.javaagent;

import com.example.scm.javaagent.asm.AsmClassPrinter;
import com.example.scm.javaagent.bytebuddy.ByteBuddyAgentInstaller;
import com.example.scm.javaagent.config.AgentConfig;
import com.example.scm.javaagent.logging.AgentLogger;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SCM Java Agent 入口，支持 JVM 启动时 premain 加载和运行时 agentmain 热挂载。
 */
public final class ScmJavaAgent {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private ScmJavaAgent() {
    }

    /**
     * JVM 启动参数 -javaagent 加载时调用。
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        install("premain", agentArgs, instrumentation);
    }

    /**
     * Attach API 运行时加载时调用。
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        install("agentmain", agentArgs, instrumentation);
    }

    private static void install(String loadMode, String agentArgs, Instrumentation instrumentation) {
        if (!INSTALLED.compareAndSet(false, true)) {
            AgentLogger.info("SCM Java Agent already installed, loadMode=" + loadMode);
            return;
        }
        AgentConfig config = AgentConfig.parse(agentArgs);
        AgentLogger.info("SCM Java Agent loaded, loadMode=" + loadMode
                + ", enabled=" + config.enabled()
                + ", includePackage=" + config.includePackage()
                + ", slowThresholdMs=" + config.slowThresholdMs());
        if (!config.enabled()) {
            AgentLogger.info("SCM Java Agent disabled by args");
            return;
        }
        AsmClassPrinter printer = new AsmClassPrinter(config);
        ByteBuddyAgentInstaller installer = new ByteBuddyAgentInstaller(config, printer);
        installer.install(instrumentation);
    }
}
