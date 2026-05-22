package com.example.scm.aiagent.tool.service;

import com.example.scm.aiagent.model.AgentRequestContext;
import com.example.scm.aiagent.tool.model.ToolCandidateFilterRequest;
import com.example.scm.aiagent.tool.model.ToolCandidateFilterResult;
import com.example.scm.aiagent.tool.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Tool 候选集过滤服务。
 *
 * <p>Phase 4.11 仅基于安全的 domain/category/routeTags/readOnly 元数据缩小候选集，
 * 为后续 Orchestrator 做准备，不承担多步规划职责。</p>
 */
@Slf4j
@Service
public class ToolCandidateFilterService {

    /**
     * 根据显式 route hint 和用户问题关键词过滤 Tool 候选集。
     */
    public ToolCandidateFilterResult filter(List<ToolDefinition> definitions,
                                            ToolCandidateFilterRequest request,
                                            AgentRequestContext context,
                                            String runId) {
        List<ToolDefinition> all = definitions == null ? List.of() : definitions;
        String resolvedDomain = resolveDomain(request);
        String resolvedCategory = normalize(request == null ? null : request.getRequestedCategory());
        List<String> resolvedTags = resolveTags(request);

        List<ToolDefinition> base = request != null && request.isReadOnlyOnly()
                ? all.stream().filter(ToolDefinition::isReadOnly).toList()
                : all;
        List<ToolDefinition> filtered = base.stream()
                .filter(tool -> !StringUtils.hasText(resolvedDomain) || resolvedDomain.equalsIgnoreCase(tool.getDomain()))
                .filter(tool -> !StringUtils.hasText(resolvedCategory) || resolvedCategory.equalsIgnoreCase(tool.getCategory()))
                .filter(tool -> resolvedTags.isEmpty() || matchesAnyTag(tool, resolvedTags))
                .toList();

        boolean fallbackUsed = filtered.isEmpty();
        List<ToolDefinition> candidates = fallbackUsed ? base : filtered;
        if (request != null && request.getMaxCandidates() > 0 && candidates.size() > request.getMaxCandidates()) {
            candidates = candidates.stream().limit(request.getMaxCandidates()).toList();
        }
        log.info("AI tool candidates filtered, tenantId={}, userId={}, runId={}, requestedDomain={}, requestedCategory={}, routeTags={}, beforeCount={}, afterCount={}, fallbackUsed={}",
                context.tenantId(), context.userId(), runId, resolvedDomain, resolvedCategory, resolvedTags,
                all.size(), candidates.size(), fallbackUsed);
        return ToolCandidateFilterResult.builder()
                .candidates(candidates)
                .beforeCount(all.size())
                .afterCount(candidates.size())
                .fallbackUsed(fallbackUsed)
                .resolvedDomain(resolvedDomain)
                .resolvedCategory(resolvedCategory)
                .resolvedRouteTags(resolvedTags)
                .build();
    }

    /**
     * 从用户问题关键词推断业务域，作为未显式传入 route hint 时的轻量补充。
     */
    public String inferDomain(String userMessage) {
        String message = normalize(userMessage);
        if (!StringUtils.hasText(message)) {
            return null;
        }
        if (message.contains("库存") || message.contains("余额") || message.contains("可用")) {
            return "inventory";
        }
        if (message.contains("销售订单") || message.contains("销售")) {
            return "sales";
        }
        if (message.contains("采购订单") || message.contains("采购")) {
            return "purchase";
        }
        if (message.contains("物料") || message.contains("仓库") || message.contains("主数据")) {
            return "mdm";
        }
        return null;
    }

    private String resolveDomain(ToolCandidateFilterRequest request) {
        if (request == null) {
            return null;
        }
        String requestedDomain = normalize(request.getRequestedDomain());
        return StringUtils.hasText(requestedDomain) ? requestedDomain : inferDomain(request.getUserMessage());
    }

    private List<String> resolveTags(ToolCandidateFilterRequest request) {
        if (request == null || request.getRouteTags() == null) {
            return List.of();
        }
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String tag : request.getRouteTags()) {
            String normalized = normalize(tag);
            if (StringUtils.hasText(normalized)) {
                tags.add(normalized);
            }
        }
        return new ArrayList<>(tags);
    }

    private boolean matchesAnyTag(ToolDefinition tool, List<String> requestedTags) {
        List<String> routeTags = tool.getRouteTags() == null ? List.of() : tool.getRouteTags();
        return routeTags.stream()
                .map(this::normalize)
                .anyMatch(requestedTags::contains);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }
}
