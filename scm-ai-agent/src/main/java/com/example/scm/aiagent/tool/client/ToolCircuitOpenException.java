package com.example.scm.aiagent.tool.client;

/**
 * Tool 轻量熔断打开时抛出的异常。
 */
public class ToolCircuitOpenException extends ToolClientException {

    public ToolCircuitOpenException(String message) {
        super(message);
    }
}
