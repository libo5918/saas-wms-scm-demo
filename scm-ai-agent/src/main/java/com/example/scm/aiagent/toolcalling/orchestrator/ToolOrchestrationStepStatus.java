package com.example.scm.aiagent.toolcalling.orchestrator;

/**
 * Tool Orchestration 单步状态。
 */
public enum ToolOrchestrationStepStatus {
    /** 已创建但尚未执行。 */
    PENDING,
    /** 正在执行 Tool。 */
    RUNNING,
    /** Tool 执行成功。 */
    SUCCESS,
    /** Tool 执行失败。 */
    FAILED,
    /** 当前步骤被跳过，预留给后续多步编排。 */
    SKIPPED
}
