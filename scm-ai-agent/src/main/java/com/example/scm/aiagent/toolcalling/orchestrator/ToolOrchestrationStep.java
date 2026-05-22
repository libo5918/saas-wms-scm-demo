package com.example.scm.aiagent.toolcalling.orchestrator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * Tool Orchestration 单步记录。
 */
@Getter
@Setter
@Builder
public class ToolOrchestrationStep {

    /** 步骤 ID，当前单步阶段按 runId-stepNo 生成。 */
    private String stepId;

    /** 步骤序号，Phase 4.12 固定为 1。 */
    private int stepNo;

    /** 本步骤选择的 Tool 名称。 */
    private String toolName;

    /** Tool 调用参数。 */
    private Map<String, Object> arguments;

    /** Planner 给出的选择原因。 */
    private String reason;

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
