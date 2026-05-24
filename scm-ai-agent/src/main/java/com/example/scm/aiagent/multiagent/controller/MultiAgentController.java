package com.example.scm.aiagent.multiagent.controller;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatRequest;
import com.example.scm.aiagent.multiagent.dto.MultiAgentChatResponse;
import com.example.scm.aiagent.multiagent.service.MultiAgentCoordinatorService;
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

/** Multi-Agent 最小演示入口，只暴露脱敏 run/step/message 状态。 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/multi-agent")
public class MultiAgentController {

    private final MultiAgentCoordinatorService coordinatorService;

    public MultiAgentController(MultiAgentCoordinatorService coordinatorService) {
        this.coordinatorService = coordinatorService;
    }

    @PostMapping("/chat")
    public Result<MultiAgentChatResponse> chat(@Valid @RequestBody MultiAgentChatRequest request,
                                               @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                               @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                               @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI multi-agent chat request received, tenantId={}, userId={}, runId={}, messageLength={}",
                context.tenantId(), context.userId(), request.getRunId(),
                request.getMessage() == null ? 0 : request.getMessage().length());
        return Result.success(coordinatorService.chat(request, context));
    }

    @GetMapping("/runs/{runId}")
    public Result<MultiAgentChatResponse> getRun(@PathVariable("runId") String runId,
                                                 @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                 @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                 @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI multi-agent run detail request received, tenantId={}, userId={}, runId={}",
                context.tenantId(), context.userId(), runId);
        return Result.success(coordinatorService.getRun(runId));
    }

    @GetMapping("/runs")
    public Result<List<MultiAgentChatResponse>> listRuns(
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
            @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI multi-agent run list request received, tenantId={}, userId={}, limit={}",
                context.tenantId(), context.userId(), limit);
        return Result.success(coordinatorService.listRuns(limit == null ? 20 : limit));
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
