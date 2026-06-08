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
        try {
            System.out.println(prefix("INFO") + sanitize(message));
        } catch (Throwable ignored) {
            // Agent 日志不能影响业务主流程。
        }
    }

    public static void warn(String message) {
        try {
            System.out.println(prefix("WARN") + sanitize(message));
        } catch (Throwable ignored) {
            // Agent 日志不能影响业务主流程。
        }
    }

    public static void error(String message, Throwable throwable) {
        try {
            System.err.println(prefix("ERROR") + sanitize(message) + ", errorType=" + throwable.getClass().getName()
                    + ", errorMessage=" + sanitize(throwable.getMessage()));
        } catch (Throwable ignored) {
            // Agent 日志不能影响业务主流程。
        }
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
                .replaceAll("(?i)(authorization)\\s*[:=]\\s*[^,\\s}]+", "$1=[REDACTED]")
                .replaceAll("(?i)(cookie)\\s*[:=]\\s*[^,\\s}]+", "$1=[REDACTED]")
                .replaceAll("(?i)(accessToken|refreshToken|token)\\s*[:=]\\s*[^,\\s}]+", "$1=[REDACTED]")
                .replaceAll("(?i)(api[-_ ]?key|apiKey)\\s*[:=]\\s*[^,\\s}]+", "$1=[REDACTED]")
                .replaceAll("(?i)(password|secret)\\s*[:=]\\s*[^,\\s}]+", "$1=[REDACTED]")
                .replaceAll("(?i)(rawData|prompt|model response)\\s*[:=]\\s*[^,}]+", "$1=[REDACTED]");
    }
}
