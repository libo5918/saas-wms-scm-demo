package com.example.scm.aiagent.workflow.controller;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowDefinitionView;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunRequest;
import com.example.scm.aiagent.workflow.dto.AgentWorkflowRunResponse;
import com.example.scm.aiagent.workflow.service.AgentWorkflowService;
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
 * Agent Workflow 演示接口。
 *
 * <p>Phase 6.1 提供固定只读业务流程，不暴露复杂工作流引擎能力。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/workflows")
public class AgentWorkflowController {

    private final AgentWorkflowService workflowService;

    public AgentWorkflowController(AgentWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    public Result<List<AgentWorkflowDefinitionView>> listDefinitions(
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
            @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI workflow definition list request received, tenantId={}, userId={}",
                context.tenantId(), context.userId());
        return Result.success(workflowService.listDefinitions());
    }

    @PostMapping("/{workflowCode}/run")
    public Result<AgentWorkflowRunResponse> run(@PathVariable("workflowCode") String workflowCode,
                                                @Valid @RequestBody AgentWorkflowRunRequest request,
                                                @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI workflow run request received, tenantId={}, userId={}, runId={}, workflowCode={}, messageLength={}",
                context.tenantId(), context.userId(), request.getRunId(), workflowCode,
                request.getMessage() == null ? 0 : request.getMessage().length());
        return Result.success(workflowService.run(workflowCode, request, context));
    }

    @GetMapping("/runs")
    public Result<List<AgentWorkflowRunResponse>> listRuns(
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit,
            @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
            @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
            @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI workflow run list request received, tenantId={}, userId={}, limit={}",
                context.tenantId(), context.userId(), limit);
        return Result.success(workflowService.listRuns(limit == null ? 20 : limit));
    }

    @GetMapping("/runs/{runId}")
    public Result<AgentWorkflowRunResponse> getRun(@PathVariable("runId") String runId,
                                                   @RequestHeader(value = GatewayHeaders.USER_ID, required = false) Long userId,
                                                   @RequestHeader(value = GatewayHeaders.USERNAME, required = false) String username,
                                                   @RequestHeader(value = GatewayHeaders.USER_ROLES, required = false) String roles) {
        AgentRequestContext context = buildContext(userId, username, roles);
        log.info("AI workflow run detail request received, tenantId={}, userId={}, runId={}",
                context.tenantId(), context.userId(), runId);
        return Result.success(workflowService.getRun(runId));
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
