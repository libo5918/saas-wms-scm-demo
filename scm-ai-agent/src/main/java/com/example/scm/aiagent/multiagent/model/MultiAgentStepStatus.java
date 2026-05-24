package com.example.scm.aiagent.multiagent.model;

/** Multi-Agent 单个步骤的执行状态。 */
public enum MultiAgentStepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED
}
