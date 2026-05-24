package com.example.scm.aiagent.mcp.controller;

import com.example.scm.aiagent.mcp.dto.McpToolInvokeRequest;
import com.example.scm.aiagent.mcp.dto.McpToolInvokeResponse;
import com.example.scm.aiagent.mcp.dto.McpToolListResponse;
import com.example.scm.aiagent.mcp.service.McpToolExposureService;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.Result;
import com.example.scm.common.core.TenantContext;
import com.example.scm.common.security.GatewayHeaders;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** MCP 风格 Tool 暴露接口。 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/mcp")
public class McpToolController {

    private final McpToolExposureService mcpToolExposureService;

    public McpToolController(McpToolExposureService mcpToolExposureService) {
        this.mcpToolExposureService = mcpToolExposureService;
    }

    @GetMapping("/tools")
    public Result<McpToolListResponse> listTools(@RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                 @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                 @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI MCP tool list request received, tenantId={}, userId={}", context.tenantId(), context.userId());
        return Result.success(mcpToolExposureService.listTools(context));
    }

    @PostMapping("/tools/{toolName}/invoke")
    public Result<McpToolInvokeResponse> invoke(@PathVariable("toolName") String toolName,
                                                @Valid @RequestBody McpToolInvokeRequest request,
                                                @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI MCP tool invoke request received, tenantId={}, userId={}, runId={}, toolName={}",
                context.tenantId(), context.userId(), request.getRunId(), toolName);
        return Result.success(mcpToolExposureService.invoke(toolName, request, context));
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
