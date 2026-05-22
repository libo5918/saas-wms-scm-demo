package com.example.scm.aiagent.tool.controller;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.dto.ToolInvocationAuditListResponse;
import com.example.scm.aiagent.tool.dto.ToolInvokeRequest;
import com.example.scm.aiagent.tool.dto.ToolListResponse;
import com.example.scm.aiagent.tool.dto.ToolResponse;
import com.example.scm.aiagent.tool.model.ToolRuntimeStatus;
import com.example.scm.aiagent.tool.service.ToolInvocationService;
import com.example.scm.aiagent.tool.service.ToolRuntimeProtectionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * AI Tools API 控制器。
 *
 * <p>提供工具列表、工具调用、审计查询和 runtime 状态查询入口，
 * 统一复用 gateway 透传的租户和用户上下文。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/tools")
public class AiToolController {

    private final ToolInvocationService toolInvocationService;
    private final ToolRuntimeProtectionService runtimeProtectionService;

    public AiToolController(ToolInvocationService toolInvocationService,
                            ToolRuntimeProtectionService runtimeProtectionService) {
        this.toolInvocationService = toolInvocationService;
        this.runtimeProtectionService = runtimeProtectionService;
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
     * 查询最近的 Tool 调用审计记录。
     */
    @GetMapping("/invocations")
    public Result<ToolInvocationAuditListResponse> listInvocations(@RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                                   @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                                   @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles,
                                                                   @RequestParam(value = "toolName", required = false) String toolName,
                                                                   @RequestParam(value = "runId", required = false) String runId,
                                                                   @RequestParam(value = "limit", required = false) Integer limit) {
        AgentRequestContext context = buildContext(userId, username, roles);
        return Result.success(toolInvocationService.listInvocations(context, toolName, runId, limit));
    }

    /**
     * 查询全部 Tool runtime 保护状态。
     */
    @GetMapping("/runtime/status")
    public Result<List<ToolRuntimeStatus>> listRuntimeStatuses(@RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                               @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                               @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI tool runtime status list request received, tenantId={}, userId={}",
                context.tenantId(), context.userId());
        return Result.success(runtimeProtectionService.listStatuses());
    }

    /**
     * 查询单个 Tool runtime 保护状态。
     */
    @GetMapping("/runtime/status/{toolName}")
    public Result<ToolRuntimeStatus> getRuntimeStatus(@PathVariable("toolName") String toolName,
                                                      @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                      @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                      @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI tool runtime status detail request received, tenantId={}, userId={}, toolName={}",
                context.tenantId(), context.userId(), toolName);
        return Result.success(runtimeProtectionService.getStatus(toolName));
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
