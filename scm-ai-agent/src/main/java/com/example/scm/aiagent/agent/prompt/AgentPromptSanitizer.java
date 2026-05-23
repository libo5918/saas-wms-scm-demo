package com.example.scm.aiagent.agent.prompt;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Prompt 上下文脱敏器。
 *
 * <p>当前阶段采用关键词白盒保护，避免把凭证、敏感头、完整原始数据等内容送入模型。</p>
 */
@Component
public class AgentPromptSanitizer {

    private static final String MASK = "[REDACTED]";
    private static final String[] SENSITIVE_KEYWORDS = {
            "rawdata", "authorization", "cookie", "token", "apikey", "api_key",
            "secret", "password", "prompt", "modelresponse", "header"
    };

    public boolean containsSensitiveKeyword(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace(" ", "");
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public Object sanitizeStructuredData(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (containsSensitiveKeyword(key)) {
                    sanitized.put(key, MASK);
                } else {
                    sanitized.put(key, sanitizeStructuredData(entry.getValue()));
                }
            }
            return sanitized;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::sanitizeStructuredData).toList();
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            java.util.List<Object> values = new java.util.ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                values.add(sanitizeStructuredData(Array.get(value, i)));
            }
            return values;
        }
        if (value instanceof String text && containsSensitiveKeyword(text)) {
            return MASK;
        }
        return value;
    }
}
