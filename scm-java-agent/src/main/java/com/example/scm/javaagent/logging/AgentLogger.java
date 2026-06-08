package com.example.scm.javaagent.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Agent 轻量日志工具，避免引入业务日志框架导致类加载冲突。
 */
public final class AgentLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private AgentLogger() {
    }

    public static void info(String message) {
        System.out.println(prefix("INFO") + message);
    }

    public static void warn(String message) {
        System.out.println(prefix("WARN") + message);
    }

    public static void error(String message, Throwable throwable) {
        System.err.println(prefix("ERROR") + message + ", errorType=" + throwable.getClass().getName()
                + ", errorMessage=" + sanitize(throwable.getMessage()));
    }

    private static String prefix(String level) {
        return "[SCM-JAVA-AGENT] " + FORMATTER.format(LocalDateTime.now()) + " " + level + " ";
    }

    /**
     * 防止异常信息中误带敏感字段。
     */
    public static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("(?i)authorization\\s*[:=]\\s*\\S+", "authorization=[REDACTED]")
                .replaceAll("(?i)cookie\\s*[:=]\\s*\\S+", "cookie=[REDACTED]")
                .replaceAll("(?i)token\\s*[:=]\\s*\\S+", "token=[REDACTED]")
                .replaceAll("(?i)api[-_ ]?key\\s*[:=]\\s*\\S+", "apiKey=[REDACTED]");
    }
}
