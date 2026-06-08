package com.example.scm.javaagent.bytebuddy;

import com.example.scm.javaagent.config.AgentConfig;

/**
 * 方法日志输出判断逻辑，便于对慢调用阈值和方法白名单做单元测试。
 */
public final class MethodTraceDecision {

    private MethodTraceDecision() {
    }

    public static boolean shouldLog(AgentConfig config, String methodName, long costMs, Throwable throwable) {
        if (config == null) {
            return false;
        }
        if (!config.shouldTraceMethod(methodName)) {
            return false;
        }
        if (throwable != null) {
            return true;
        }
        return config.slowThresholdMs() <= 0 || costMs >= config.slowThresholdMs();
    }
}
