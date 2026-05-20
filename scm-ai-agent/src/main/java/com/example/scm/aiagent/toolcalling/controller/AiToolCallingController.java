package com.example.scm.aiagent.toolcalling.controller;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteRequest;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingExecuteResponse;
import com.example.scm.aiagent.toolcalling.dto.ToolCallingSchemaListResponse;
import com.example.scm.aiagent.toolcalling.service.SpringAiToolCallingService;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.Result;
import com.example.scm.common.core.TenantContext;
import com.example.scm.common.security.GatewayHeaders;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Spring AI Tool Calling 调试接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/tool-calling")
public class AiToolCallingController {

    private final SpringAiToolCallingService springAiToolCallingService;

    public AiToolCallingController(SpringAiToolCallingService springAiToolCallingService) {
        this.springAiToolCallingService = springAiToolCallingService;
    }

    /**
     * 查询当前模型可见的 Tool schema 列表。
     */
    @GetMapping("/schema")
    public Result<ToolCallingSchemaListResponse> listSchemas(@RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                             @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                             @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        return Result.success(springAiToolCallingService.listSchemas(context));
    }

    /**
     * 模拟模型返回 Tool 调用指令后执行服务端 Tool。
     */
    @PostMapping("/execute")
    public Result<ToolCallingExecuteResponse> execute(@Valid @RequestBody ToolCallingExecuteRequest request,
                                                      @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                      @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                      @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI tool calling execute request received, tenantId={}, userId={}, runId={}, toolName={}",
                context.tenantId(), context.userId(), request.getRunId(), request.getToolName());
        return Result.success(springAiToolCallingService.execute(request, context));
    }

    private AgentRequestContext buildContext(Long userId, String username, String roles) {
        Long tenantId = TenantContext.getRequiredTenantId();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED.code(), "Missing user context");
        }
        return new AgentRequestContext(tenantId, userId, username, parseRoles(roles));
    }

    private List<String> parseRoles(String roles) {
        if (!StringUtils.hasText(roles)) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
