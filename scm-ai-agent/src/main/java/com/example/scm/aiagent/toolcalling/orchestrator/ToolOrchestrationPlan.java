package com.example.scm.aiagent.toolcalling.orchestrator;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Orchestrator 显式计划模型。
 *
 * <p>Phase 4.13 默认只构造 SINGLE_STEP 计划；dry-run 模式可表达多步骤但不执行后续真实 Tool。</p>
 */
@Getter
@Builder
public class ToolOrchestrationPlan {

    /** 计划 ID。 */
    private String planId;

    /** 运行 ID，与 Tool Calling Chat runId 对齐。 */
    private String runId;

    /** 计划模式。 */
    private ToolOrchestrationPlanMode mode;

    /** 用户目标摘要。 */
    private String objective;

    /** 计划步骤。 */
    private List<ToolOrchestrationStep> steps;

    /** 本计划允许的最大步骤数。 */
    private int maxSteps;

    /** 计划来源，例如 service-single-step。 */
    private String generatedBy;

    /** 计划创建时间。 */
    private Instant createdAt;
}
