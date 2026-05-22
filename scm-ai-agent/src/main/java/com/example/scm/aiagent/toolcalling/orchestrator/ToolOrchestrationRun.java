package com.example.scm.aiagent.toolcalling.orchestrator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Tool Calling Orchestration 运行记录。
 *
 * <p>Phase 4.12 仅记录当前单步 Tool Calling 闭环，为后续多步编排保留结构。</p>
 */
@Getter
@Setter
@Builder
public class ToolOrchestrationRun {

    /** 运行 ID，与 Tool Calling Chat runId 对齐。 */
    private String runId;

    /** 租户 ID。 */
    private Long tenantId;

    /** 用户 ID。 */
    private Long userId;

    /** 用户原始问题，仅用于调试定位，不在日志中打印全文。 */
    private String userMessage;

    /** Planner 模式。 */
    private String plannerMode;

    /** Answer 生成模式。 */
    private String answerMode;

    /** 用户显式指定的 Tool。 */
    private String requestedTool;

    /** route hint 业务域。 */
    private String requestedDomain;

    /** route hint 类别。 */
    private String requestedCategory;

    /** route hint 标签。 */
    private List<String> routeTags;

    /** 当前运行包含的步骤，Phase 4.12 只会有一个步骤。 */
    private List<ToolOrchestrationStep> steps;

    /** 最终返回给用户的答案。 */
    private String finalAnswer;

    /** 当前 run 是否成功。 */
    private boolean success;

    /** 创建时间。 */
    private Instant createdAt;

    /** 完成时间。 */
    private Instant finishedAt;

    /** 整体耗时，单位毫秒。 */
    private long latencyMs;
}
