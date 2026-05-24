package com.example.scm.aiagent.multiagent;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.model.MultiAgentIntentType;
import com.example.scm.aiagent.multiagent.model.MultiAgentPlan;
import com.example.scm.aiagent.multiagent.service.MultiAgentToolService;
import com.example.scm.aiagent.toolcalling.application.ToolCallingChatService;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingChatResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecutionView;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayData;
import com.example.scm.aiagent.toolcalling.model.ToolCallingDisplayField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiAgentToolServiceTest {

    @Test
    void shouldUseToolCallingChatServiceAndReturnSafeSummary() {
        ToolCallingChatService chatService = mock(ToolCallingChatService.class);
        when(chatService.chat(any(), any())).thenReturn(ToolCallingChatResponse.builder()
                .runId("run-tool")
                .selectedTool("mdm.getMaterial")
                .toolArguments(Map.of("materialCode", "MAT-001"))
                .planningSource("mock")
                .execution(ToolCallingExecutionView.builder()
                        .success(true)
                        .toolName("mdm.getMaterial")
                        .data(ToolCallingDisplayData.builder()
                                .displayTitle("物料信息")
                                .displaySummary("已查询到物料 MAT-001")
                                .displayFields(List.of(ToolCallingDisplayField.builder()
                                        .key("materialCode")
                                        .label("物料编码")
                                        .value("MAT-001")
                                        .build()))
                                .displayItems(List.of())
                                .rawData(Map.of("authorization", "secret"))
                                .build())
                        .latencyMs(10)
                        .build())
                .answer("查询成功")
                .latencyMs(12)
                .build());

        MultiAgentToolService service = new MultiAgentToolService(chatService);
        Map<String, Object> result = service.execute(request(), context(), toolPlan(), "run-tool", true);

        assertEquals("SUCCESS", result.get("status"));
        Map<?, ?> execution = (Map<?, ?>) result.get("execution");
        assertEquals("物料信息", execution.get("displayTitle"));
        assertFalse(result.toString().toLowerCase().contains("rawdata"));
        assertFalse(result.toString().toLowerCase().contains("authorization"));
    }

    private MultiAgentChatRequest request() {
        MultiAgentChatRequest request = new MultiAgentChatRequest();
        request.setMessage("帮我查物料 MAT-001");
        return request;
    }

    private MultiAgentPlan toolPlan() {
        return MultiAgentPlan.builder()
                .intentType(MultiAgentIntentType.TOOL_ONLY)
                .needTool(true)
                .needReview(true)
                .reason("工具查询")
                .build();
    }

    private AgentRequestContext context() {
        return new AgentRequestContext(1L, 10001L, "admin", List.of("ROLE_ADMIN"));
    }
}
