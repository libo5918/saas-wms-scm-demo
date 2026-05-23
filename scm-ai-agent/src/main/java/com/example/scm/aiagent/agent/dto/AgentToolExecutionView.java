package com.example.scm.aiagent.agent.dto;

import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayField;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayItem;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** Tool execution 的安全视图，不包含 rawData。 */
@Getter
@Builder
public class AgentToolExecutionView {

    private boolean success;
    private String toolName;
    private String errorCode;
    private String errorMessage;
    private String displayTitle;
    private String displaySummary;
    private List<ToolCallingDisplayField> displayFields;
    private List<ToolCallingDisplayItem> displayItems;
    private long latencyMs;
}
