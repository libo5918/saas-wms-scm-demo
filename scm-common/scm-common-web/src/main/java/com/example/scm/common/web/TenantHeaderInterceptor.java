package com.example.scm.common.web;

import com.example.scm.common.core.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.HandlerInterceptor;

public class TenantHeaderInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String GATEWAY_INTERNAL_HEADER = "X-Gateway-Internal";
    public static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    private final Environment environment;
    private final boolean enforceGateway;
    private final String internalSecret;

    public TenantHeaderInterceptor(
            Environment environment,
            @Value("${security.gateway.enforce:false}") boolean enforceGateway,
            @Value("${security.gateway.internal-secret:}") String internalSecret
    ) {
        this.environment = environment;
        this.enforceGateway = enforceGateway;
        this.internalSecret = internalSecret;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (shouldEnforceGateway(request)) {
            String gatewayInternal = request.getHeader(GATEWAY_INTERNAL_HEADER);
            String gatewaySecret = request.getHeader(GATEWAY_SECRET_HEADER);
            if (!"true".equalsIgnoreCase(gatewayInternal) || !internalSecret.equals(gatewaySecret)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Gateway access required");
                return false;
            }
        }

        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setTenantId(Long.parseLong(tenantId));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }

    private boolean shouldEnforceGateway(HttpServletRequest request) {
        if (!enforceGateway) {
            return false;
        }
        if (isLocalProfileActive()) {
            return false;
        }
        String uri = request.getRequestURI();
        return !(uri.startsWith("/actuator")
                || uri.startsWith("/swagger")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/doc.html")
                || uri.startsWith("/favicon.ico"));
    }

    private boolean isLocalProfileActive() {
        for (String profile : environment.getActiveProfiles()) {
            if ("local".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
