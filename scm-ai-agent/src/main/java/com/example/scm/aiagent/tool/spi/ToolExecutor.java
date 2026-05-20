package com.example.scm.aiagent.tool.spi;

import com.example.scm.aiagent.tool.model.ToolDefinition;
import com.example.scm.aiagent.tool.model.ToolRequest;

/**
 * Agent Tool 执行器抽象。
 */
public interface ToolExecutor {

    /**
     * 返回工具定义，用于注册和对外展示。
     */
    ToolDefinition definition();

    /**
     * 执行工具并返回结构化数据。
     */
    Object execute(ToolRequest request);
}
