package com.example.scm.aiagent.tool.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Tool 调用请求。
 *
 * <p>tenantId 和 userId 不允许从请求体传入，必须使用 gateway 透传的身份上下文。</p>
 */
@Getter
@Setter
public class ToolInvokeRequest {

    /** 工具名称，例如 inventory.getBalance。 */
    @NotBlank(message = "toolName is required")
    private String toolName;

    /** 本次 Agent 运行 ID；为空时服务端会自动生成。 */
    private String runId;

    /** 工具参数，当前 Phase 4 仅用于 mock/local adapter。 */
    private Map<String, Object> parameters = new HashMap<>();
}
