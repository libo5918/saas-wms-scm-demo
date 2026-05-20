package com.example.scm.aiagent.tool.client;

/**
 * Tool 业务服务客户端异常。
 *
 * <p>用于把远程 SCM/WMS 服务调用失败统一转换为 ToolResponse 中的失败信息。</p>
 */
public class ToolClientException extends RuntimeException {

    public ToolClientException(String message) {
        super(message);
    }

    public ToolClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
