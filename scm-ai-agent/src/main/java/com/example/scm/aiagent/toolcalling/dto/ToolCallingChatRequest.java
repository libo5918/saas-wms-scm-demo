package com.example.scm.aiagent.toolcalling.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool Calling Chat 请求。
 */
@Getter
@Setter
public class ToolCallingChatRequest {

    /** 用户输入的问题。 */
    @NotBlank(message = "message must not be blank")
    private String message;

    /** 本次运行 ID。 */
    private String runId;

    /** planner 模式，允许显式覆盖默认配置。 */
    private String plannerMode;

    /** 显式指定的目标工具，优先级高于模型规划。 */
    private String requestedTool;

    /** 显式传入的工具参数。 */
    private Map<String, Object> toolArguments = new HashMap<>();

    /** 可选路由提示：期望的业务域，例如 inventory、mdm、sales、purchase。 */
    private String requestedDomain;

    /** 可选路由提示：期望的工具类别，例如 query。 */
    private String requestedCategory;

    /** 可选路由提示：期望匹配的 routeTags。 */
    private List<String> routeTags;
}
