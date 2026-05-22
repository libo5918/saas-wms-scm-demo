package com.example.scm.aiagent.toolcalling.orchestrator;

import org.springframework.stereotype.Component;

/**
 * Orchestration step 引用构建器。
 *
 * <p>引用只指向安全摘要字段，不提供 rawData、prompt 或模型响应的解析入口。</p>
 */
@Component
public class ToolOrchestrationStepRefBuilder {

    /** 生成稳定 step 引用名，例如 step-1。 */
    public String stepRef(int stepNo) {
        return "step-" + stepNo;
    }

    /** 生成输出摘要引用路径，不指向 rawData。 */
    public String outputRef(int zeroBasedIndex) {
        return "$.steps[" + zeroBasedIndex + "].outputSummary";
    }

    /** 生成前置步骤输入引用，例如 step-1.outputSummary。 */
    public String outputSummaryInputRef(int stepNo) {
        return stepRef(stepNo) + ".outputSummary";
    }
}
