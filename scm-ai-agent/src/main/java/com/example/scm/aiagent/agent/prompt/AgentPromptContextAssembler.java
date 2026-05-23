package com.example.scm.aiagent.agent.prompt;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Agent prompt 上下文组装器。
 *
 * <p>统一收集 Provider 片段，并完成优先级排序、长度裁剪和敏感片段过滤。</p>
 */
@Component
public class AgentPromptContextAssembler {

    private final List<AgentPromptContextProvider> providers;
    private final AgentPromptSanitizer sanitizer;

    public AgentPromptContextAssembler(List<AgentPromptContextProvider> providers,
                                       AgentPromptSanitizer sanitizer) {
        this.providers = providers;
        this.sanitizer = sanitizer;
    }

    public AgentPromptContext assemble(AgentPromptBuildRequest request) {
        List<AgentPromptSection> sections = providers.stream()
                .flatMap(provider -> provider.provide(request).stream())
                .map(this::normalize)
                .sorted(Comparator.comparingInt(AgentPromptSection::getPriority))
                .toList();
        return AgentPromptContext.builder()
                .runId(request.getRunId())
                .intentType(request.getIntentType())
                .sections(sections)
                .build();
    }

    @SuppressWarnings("unchecked")
    private AgentPromptSection normalize(AgentPromptSection section) {
        boolean sensitive = section.isSensitive()
                || sanitizer.containsSensitiveKeyword(section.getTitle())
                || sanitizer.containsSensitiveKeyword(section.getContent());
        String content = section.getContent();
        int maxLength = section.getMaxLength();
        boolean truncated = section.isTruncated();
        if (!sensitive && maxLength > 0 && StringUtils.hasText(content) && content.length() > maxLength) {
            content = content.substring(0, maxLength) + "...";
            truncated = true;
        }
        Object structuredData = sanitizer.sanitizeStructuredData(section.getStructuredData());
        return AgentPromptSection.builder()
                .type(section.getType())
                .source(section.getSource())
                .title(section.getTitle())
                .content(content)
                .structuredData(structuredData instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of())
                .priority(section.getPriority())
                .maxLength(maxLength)
                .included(!sensitive && section.isIncluded())
                .truncated(truncated)
                .sensitive(sensitive)
                .build();
    }
}
