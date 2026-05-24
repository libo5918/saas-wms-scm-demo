package com.example.scm.aiagent.multiagent.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/** Agent 间消息的安全摘要，不保存完整 prompt、模型响应或 rawData。 */
@Getter
@Setter
@Builder
public class MultiAgentMessage {

    private String messageId;
    private String fromAgent;
    private String toAgent;
    private MultiAgentMessageType messageType;
    private String contentSummary;
    private Map<String, Object> structuredData;
    private Instant createdAt;
}
