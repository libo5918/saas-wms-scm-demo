package com.example.scm.aiagent.toolcalling.orchestrator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Tool Orchestration 单个步骤记录。
 */
@Getter
@Setter
@Builder
public class ToolOrchestrationStep {

    /** 步骤 ID，当前按 runId-stepNo 生成。 */
    private String stepId;

    /** 步骤序号。 */
    private int stepNo;

    /** 本步骤计划调用的 Tool 名称。 */
    private String toolName;

    /** Tool 调用参数；状态接口仅用于调试，不承载敏感头或 token。 */
    private Map<String, Object> arguments;

    /** Planner 或 Orchestrator 给出的选择原因。 */
    private String reason;

    /** 当前步骤依赖的前置 stepId。 */
    private List<String> dependsOnStepIds;

    /** 当前步骤可读取的前置上下文摘要，不包含完整 rawData。 */
    private String inputSummary;

    /** 当前步骤执行完成后的安全输出摘要，不包含完整 rawData。 */
    private String outputSummary;

    /** dry-run 或失败中止时的跳过原因。 */
    private String skipReason;

    /** 步骤状态。 */
    private ToolOrchestrationStepStatus status;

    /** 脱敏后的执行摘要。 */
    private ToolOrchestrationExecutionSummary execution;

    /** 步骤开始时间。 */
    private Instant startedAt;

    /** 步骤结束时间。 */
    private Instant finishedAt;

    /** 步骤耗时，单位毫秒。 */
    private long latencyMs;
}
