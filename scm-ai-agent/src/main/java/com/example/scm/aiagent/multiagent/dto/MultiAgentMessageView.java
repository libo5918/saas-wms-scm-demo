package com.example.scm.aiagent.multiagent.dto;

import com.example.scm.aiagent.multiagent.model.MultiAgentMessageType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/** Agent 间安全摘要消息视图。 */
@Getter
@Builder
public class MultiAgentMessageView {

    private String fromAgent;
    private String toAgent;
    private MultiAgentMessageType messageType;
    private String contentSummary;
    private Map<String, Object> structuredData;
}
