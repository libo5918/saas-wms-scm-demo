package com.example.scm.aiagent.rag.dto;

import com.example.scm.aiagent.dto.ChatResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG Chat 响应。
 */
@Getter
@Setter
public class RagChatResponse {

    /** 模型回答及模型路由信息。 */
    private ChatResponse chat;

    /** 本次用于增强上下文的引用切片。 */
    private List<RagRetrievedChunk> citations = new ArrayList<>();

    /** 本次检索命中的切片数量。 */
    private int retrievalCount;

    /** RAG 总耗时，单位毫秒。 */
    private long latencyMs;
}
