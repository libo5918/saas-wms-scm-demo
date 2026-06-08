package com.example.scm.javaagent.bytebuddy;

import com.example.scm.javaagent.logging.AgentLogger;
import net.bytebuddy.asm.Advice;

/**
 * Byte Buddy Advice，记录方法耗时和异常类型，不打印入参和返回大对象。
 */
public final class MethodTimingAdvice {

    private MethodTimingAdvice() {
    }

    @Advice.OnMethodEnter
    public static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Origin("#t") String className,
                            @Advice.Origin("#m") String methodName,
                            @Advice.Enter long startNanos,
                            @Advice.Thrown Throwable throwable) {
        long costMs = (System.nanoTime() - startNanos) / 1_000_000;
        boolean success = throwable == null;
        StringBuilder message = new StringBuilder()
                .append("method=").append(className).append('#').append(methodName)
                .append(", costMs=").append(costMs)
                .append(", success=").append(success);
        if (throwable != null) {
            message.append(", errorType=").append(throwable.getClass().getName())
                    .append(", errorMessage=").append(AgentLogger.sanitize(throwable.getMessage()));
        }
        AgentLogger.info(message.toString());
    }
}
