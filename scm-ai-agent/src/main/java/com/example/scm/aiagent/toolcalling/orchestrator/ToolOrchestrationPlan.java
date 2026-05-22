package com.example.scm.aiagent.toolcalling.orchestrator;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Orchestrator 规划结果占位模型。
 *
 * <p>Phase 4.12 只保存单步计划，后续多轮 Tool Calling 可扩展为多 step。</p>
 */
@Getter
@Builder
public class ToolOrchestrationPlan {

    /** 运行 ID。 */
    private String runId;

    /** 计划步骤列表。 */
    private List<ToolOrchestrationStep> steps;
}
