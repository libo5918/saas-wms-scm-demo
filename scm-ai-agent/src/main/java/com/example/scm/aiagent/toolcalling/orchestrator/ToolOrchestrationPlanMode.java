package com.example.scm.aiagent.toolcalling.orchestrator;

/**
 * Orchestration plan 的受控执行模式。
 */
public enum ToolOrchestrationPlanMode {
    /** 默认单步计划，只执行当前 Tool Calling Chat 选中的一个 Tool。 */
    SINGLE_STEP,
    /** 多步骤 dry-run 计划，只表达后续步骤并标记跳过，不执行第二个及后续真实 Tool。 */
    MULTI_STEP_DRY_RUN,
    /** 预留给后续受控多步骤执行，本阶段不作为默认主路径。 */
    MULTI_STEP_CONTROLLED
}
