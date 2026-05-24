package com.example.scm.aiagent.mcp.controller;

import com.example.scm.aiagent.mcp.dto.McpJsonRpcRequest;
import com.example.scm.aiagent.mcp.dto.McpJsonRpcResponse;
import com.example.scm.aiagent.mcp.service.McpServerTransportService;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import com.example.scm.common.core.TenantContext;
import com.example.scm.common.security.GatewayHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** 标准 MCP Server HTTP JSON-RPC 最小端点。 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/mcp")
public class McpServerController {

    private final McpServerTransportService mcpServerTransportService;

    public McpServerController(McpServerTransportService mcpServerTransportService) {
        this.mcpServerTransportService = mcpServerTransportService;
    }

    @PostMapping("/server")
    public McpJsonRpcResponse handle(@RequestBody McpJsonRpcRequest request,
                                     @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                     @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                     @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI MCP server request received, tenantId={}, userId={}, mcpMethod={}",
                context.tenantId(), context.userId(), request == null ? null : request.getMethod());
        return mcpServerTransportService.handle(request, context);
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
