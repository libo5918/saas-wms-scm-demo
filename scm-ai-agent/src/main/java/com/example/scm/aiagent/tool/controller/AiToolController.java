package com.example.scm.aiagent.tool.controller;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolInvokeRequest;
import com.example.scm.aiagent.tool.dto.ToolListResponse;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
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
 * AI Tools API 控制器。
 *
 * <p>提供工具列表和工具调用入口，统一复用 gateway 透传的租户和用户上下文。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/tools")
public class AiToolController {

    private final ToolInvocationService toolInvocationService;

    public AiToolController(ToolInvocationService toolInvocationService) {
        this.toolInvocationService = toolInvocationService;
    }

    /**
     * 查询当前可用 Tool 列表。
     */
    @GetMapping
    public Result<ToolListResponse> listTools(@RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                              @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                              @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI tools list request received, tenantId={}, userId={}", context.tenantId(), context.userId());
        return Result.success(toolInvocationService.listTools(context));
    }

    /**
     * 调用指定 Tool。
     */
    @PostMapping("/invoke")
    public Result<ToolResponse> invoke(@Valid @RequestBody ToolInvokeRequest request,
                                       @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                       @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                       @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI tool invoke request received, tenantId={}, userId={}, runId={}, toolName={}",
                context.tenantId(), context.userId(), request.getRunId(), request.getToolName());
        return Result.success(toolInvocationService.invoke(request, context));
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
