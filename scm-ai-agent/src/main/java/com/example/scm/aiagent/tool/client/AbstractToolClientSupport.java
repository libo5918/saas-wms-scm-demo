package com.example.scm.aiagent.tool.client;

import com.example.scm.aiagent.tool.model.ToolRequest;
import com.example.scm.common.security.GatewayHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Tool Client 公共辅助逻辑。
 *
 * <p>集中处理参数读取和网关身份头透传，避免每个 HTTP adapter 重复实现。</p>
 */
public abstract class AbstractToolClientSupport {

    protected Long longParam(Map<String, Object> parameters, String name, Long defaultValue) {
        Object value = parameters == null ? null : parameters.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    protected String stringParam(Map<String, Object> parameters, String name, String defaultValue) {
        Object value = parameters == null ? null : parameters.get(name);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    /**
     * 构造下游业务服务需要的身份透传请求头。
     */
    protected HttpHeaders identityHeaders(ToolRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(GatewayHeaders.TENANT_ID, String.valueOf(request.getContext().tenantId()));
        headers.add(GatewayHeaders.USER_ID, String.valueOf(request.getContext().userId()));
        headers.add(GatewayHeaders.USERNAME, request.getContext().username());
        headers.add(GatewayHeaders.USER_ROLES, String.join(",", request.getContext().roles()));
        headers.add("X-Agent-Run-Id", request.getRunId());
        return headers;
    }
}
