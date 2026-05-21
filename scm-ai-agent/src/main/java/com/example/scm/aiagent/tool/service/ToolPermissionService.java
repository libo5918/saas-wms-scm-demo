package com.example.scm.aiagent.tool.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tool 权限治理服务。
 *
 * <p>Phase 4.10 先提供只读 Tool 的最小权限闭环：默认保持本地联调放行，
 * 开启 strictEnabled 后再基于上下文角色/权限标签执行校验。</p>
 */
@Service
public class ToolPermissionService {

    private final AiAgentProperties properties;

    public ToolPermissionService(AiAgentProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断当前用户上下文是否允许调用指定 Tool。
     */
    public ToolPermissionDecision authorize(ToolDefinition definition, AgentRequestContext context) {
        AiAgentProperties.AccessControlProperties accessControl = properties.getTools().getAccessControl();
        List<String> principals = context.roles() == null ? List.of() : context.roles();
        if (accessControl.getAdminRoles().stream().anyMatch(principals::contains)) {
            return ToolPermissionDecision.allow("admin_role");
        }
        if (!accessControl.isStrictEnabled()) {
            if (definition.isReadOnly() && accessControl.isDefaultAllowReadOnly()) {
                return ToolPermissionDecision.allow("default_read_only");
            }
            return ToolPermissionDecision.allow("strict_disabled");
        }
        List<String> requiredRoles = definition.getRequiredRoles() == null ? List.of() : definition.getRequiredRoles();
        if (!requiredRoles.isEmpty() && requiredRoles.stream().noneMatch(principals::contains)) {
            return ToolPermissionDecision.deny("missing_role");
        }
        List<String> requiredPermissions = definition.getRequiredPermissions() == null ? List.of() : definition.getRequiredPermissions();
        if (!requiredPermissions.isEmpty() && requiredPermissions.stream().noneMatch(principals::contains)) {
            return ToolPermissionDecision.deny("missing_permission");
        }
        return ToolPermissionDecision.allow("matched");
    }

    public record ToolPermissionDecision(boolean allowed, String reason) {
        public static ToolPermissionDecision allow(String reason) {
            return new ToolPermissionDecision(true, reason);
        }

        public static ToolPermissionDecision deny(String reason) {
            return new ToolPermissionDecision(false, reason);
        }
    }
}
