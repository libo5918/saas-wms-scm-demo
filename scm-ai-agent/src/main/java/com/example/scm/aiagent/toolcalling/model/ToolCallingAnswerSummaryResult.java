package com.example.scm.aiagent.toolcalling.model;

import lombok.Builder;

/**
 * Tool Calling Chat 最终答案生成结果。
 *
 * <p>用于承载二阶段回答生成的输出，明确记录最终答案、实际使用的 answerMode
 * 以及是否发生了回退，便于日志记录和后续演进。</p>
 */
@Builder
public record ToolCallingAnswerSummaryResult(
        /**
         * 最终返回给用户的中文答案。
         */
        String answer,

        /**
         * 实际生效的答案生成模式，例如 template、spring-ai。
         */
        String answerMode,

        /**
         * 是否发生了从 spring-ai 回退到 template。
         */
        boolean fallbackUsed
) {
}
