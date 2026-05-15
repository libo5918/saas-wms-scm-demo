package com.example.scm.aiagent.service;

import com.example.scm.aiagent.config.AiAgentProperties;
import com.example.scm.aiagent.config.AiAgentProperties.ModelProperties;
import com.example.scm.aiagent.config.AiAgentProperties.ProviderProperties;
import com.example.scm.aiagent.model.ModelRoute;
import com.example.scm.aiagent.model.ModelRouteRequest;
import com.example.scm.common.core.BusinessException;
import com.example.scm.common.core.CommonErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 默认模型路由器。
 *
 * <p>当前阶段只负责模型选择和 provider 元数据返回，不在这里直接调用模型。
 * 路由顺序为：显式指定模型、任务类型、能力标签、默认模型。</p>
 */
@Service
public class DefaultModelRouter implements ModelRouter {

    private final AiAgentProperties properties;

    public DefaultModelRouter(AiAgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public ModelRoute route(ModelRouteRequest request) {
        List<ModelProperties> models = enabledModels();
        Map<String, ProviderProperties> providers = enabledProvidersByName();
        if (models.isEmpty()) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR.code(), "No enabled AI model configured");
        }

        String providerMode = normalizeProviderMode(request.providerMode());
        Predicate<ModelProperties> available = model -> isAvailable(model, providers, providerMode, request);

        if (StringUtils.hasText(request.requestedModel())) {
            ModelProperties requested = findByName(models, request.requestedModel())
                    .orElseThrow(() -> new BusinessException(
                            CommonErrorCode.BAD_REQUEST.code(),
                            "Requested model is not configured or disabled: " + request.requestedModel()));
            if (!available.test(requested)) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                        "Requested model is not available for current route constraints: " + request.requestedModel());
            }
            return toRoute(requested, providers, providerMode, "requested_model");
        }

        String taskType = normalize(request.taskType());
        Optional<ModelProperties> taskMatched = models.stream()
                .filter(available)
                .filter(model -> containsIgnoreCase(model.getTaskTypes(), taskType))
                .sorted(modelComparator())
                .findFirst();
        if (taskMatched.isPresent()) {
            return toRoute(taskMatched.get(), providers, providerMode, "task_type:" + taskType);
        }

        Optional<ModelProperties> capabilityMatched = models.stream()
                .filter(available)
                .filter(model -> hasRequiredCapabilities(model, request.requiredCapabilities()))
                .sorted(modelComparator())
                .findFirst();
        if (capabilityMatched.isPresent()) {
            return toRoute(capabilityMatched.get(), providers, providerMode, "capabilities");
        }

        ModelProperties defaultModel = findByName(models, properties.getDefaultModel())
                .filter(available)
                .orElseGet(() -> models.stream()
                        .filter(available)
                        .sorted(modelComparator())
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST.code(),
                                "No AI model is available for current route constraints")));
        return toRoute(defaultModel, providers, providerMode, "default_model");
    }

    private List<ModelProperties> enabledModels() {
        List<ModelProperties> configured = properties.getModels();
        if (configured == null || configured.isEmpty()) {
            configured = defaultModels();
        }
        return configured.stream()
                .filter(ModelProperties::isEnabled)
                .filter(model -> StringUtils.hasText(model.getName()))
                .toList();
    }

    private Map<String, ProviderProperties> enabledProvidersByName() {
        List<ProviderProperties> configured = properties.getProviders();
        if (configured == null || configured.isEmpty()) {
            configured = defaultProviders();
        }
        return configured.stream()
                .filter(ProviderProperties::isEnabled)
                .filter(provider -> StringUtils.hasText(provider.getName()))
                .collect(Collectors.toMap(
                        provider -> provider.getName().toLowerCase(Locale.ROOT),
                        provider -> provider,
                        (left, right) -> left));
    }

    private List<ProviderProperties> defaultProviders() {
        List<ProviderProperties> defaults = new ArrayList<>();
        defaults.add(provider("mock", "mock", null, null));
        defaults.add(provider("dashscope", "dashscope",
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "DASHSCOPE_API_KEY"));
        defaults.add(provider("openai", "openai-compatible",
                "https://api.openai.com/v1", "OPENAI_API_KEY"));
        defaults.add(provider("deepseek", "openai-compatible",
                "https://api.deepseek.com", "DEEPSEEK_API_KEY"));
        return defaults;
    }

    private List<ModelProperties> defaultModels() {
        List<ModelProperties> defaults = new ArrayList<>();
        defaults.add(model("qwen-plus", "qwen-plus", "dashscope", "medium",
                List.of("mock", "spring-ai"),
                List.of("CHAT", "RAG", "TOOL_CALLING", "STRUCTURED_OUTPUT"),
                List.of("simple_chat", "rag_qa", "tool_calling"),
                List.of("qwen-turbo", "chatgpt-main"),
                20));
        defaults.add(model("qwen-turbo", "qwen-turbo", "dashscope", "low",
                List.of("mock", "spring-ai"),
                List.of("CHAT", "LOW_COST"),
                List.of("summary"),
                List.of("chatgpt-main"),
                10));
        defaults.add(model("chatgpt-main", "gpt-4.1-mini", "openai", "high",
                List.of("mock", "spring-ai"),
                List.of("CHAT", "TOOL_CALLING", "STRUCTURED_OUTPUT", "PLANNING", "HIGH_QUALITY"),
                List.of("workflow_planning", "complex_reasoning"),
                List.of("qwen-plus", "deepseek-chat"),
                30));
        defaults.add(model("deepseek-chat", "deepseek-chat", "deepseek", "medium",
                List.of("mock", "spring-ai"),
                List.of("CHAT", "STRUCTURED_OUTPUT", "PLANNING"),
                List.of("complex_reasoning", "code_reasoning"),
                List.of("qwen-plus"),
                40));
        return defaults;
    }

    private ProviderProperties provider(String name, String type, String baseUrl, String apiKeyEnv) {
        ProviderProperties provider = new ProviderProperties();
        provider.setName(name);
        provider.setType(type);
        provider.setBaseUrl(baseUrl);
        provider.setApiKeyEnv(apiKeyEnv);
        return provider;
    }

    private ModelProperties model(String name, String providerModel, String provider, String costLevel,
                                  List<String> providerModes, List<String> capabilities,
                                  List<String> taskTypes, List<String> fallbackModels, int priority) {
        ModelProperties model = new ModelProperties();
        model.setName(name);
        model.setProviderModel(providerModel);
        model.setProvider(provider);
        model.setCostLevel(costLevel);
        model.setProviderModes(providerModes);
        model.setCapabilities(capabilities);
        model.setTaskTypes(taskTypes);
        model.setFallbackModels(fallbackModels);
        model.setPriority(priority);
        return model;
    }

    private Optional<ModelProperties> findByName(List<ModelProperties> models, String name) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }
        return models.stream()
                .filter(model -> name.equalsIgnoreCase(model.getName()))
                .findFirst();
    }

    private boolean containsIgnoreCase(List<String> values, String expected) {
        if (!StringUtils.hasText(expected) || values == null) {
            return false;
        }
        return values.stream().anyMatch(value -> expected.equalsIgnoreCase(value));
    }

    private boolean hasRequiredCapabilities(ModelProperties model, List<String> requiredCapabilities) {
        if (requiredCapabilities == null || requiredCapabilities.isEmpty()) {
            return true;
        }
        return requiredCapabilities.stream()
                .filter(StringUtils::hasText)
                .allMatch(required -> containsIgnoreCase(model.getCapabilities(), required.trim()));
    }

    private boolean isAvailable(ModelProperties model, Map<String, ProviderProperties> providers,
                                String providerMode, ModelRouteRequest request) {
        if (!hasProvider(model, providers)) {
            return false;
        }
        if (!supportsProviderMode(model, providerMode)) {
            return false;
        }
        if (StringUtils.hasText(request.costLevel())
                && !request.costLevel().equalsIgnoreCase(model.getCostLevel())) {
            return false;
        }
        return request.maxLatencyMs() == null
                || model.getMaxLatencyMs() == null
                || model.getMaxLatencyMs() <= request.maxLatencyMs();
    }

    private boolean hasProvider(ModelProperties model, Map<String, ProviderProperties> providers) {
        return StringUtils.hasText(model.getProvider())
                && providers.containsKey(model.getProvider().toLowerCase(Locale.ROOT));
    }

    private boolean supportsProviderMode(ModelProperties model, String providerMode) {
        List<String> providerModes = model.getProviderModes();
        return providerModes == null
                || providerModes.isEmpty()
                || providerModes.stream().anyMatch(mode -> providerMode.equalsIgnoreCase(mode));
    }

    private Comparator<ModelProperties> modelComparator() {
        return Comparator.comparingInt(ModelProperties::getPriority)
                .thenComparing(ModelProperties::getName, String.CASE_INSENSITIVE_ORDER);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "simple_chat";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeProviderMode(String providerMode) {
        if (StringUtils.hasText(providerMode)) {
            return providerMode.trim().toLowerCase(Locale.ROOT);
        }
        if (StringUtils.hasText(properties.getProviderMode())) {
            return properties.getProviderMode().trim().toLowerCase(Locale.ROOT);
        }
        return "mock";
    }

    private ModelRoute toRoute(ModelProperties model, Map<String, ProviderProperties> providers,
                               String providerMode, String reason) {
        ProviderProperties provider = providers.get(model.getProvider().toLowerCase(Locale.ROOT));
        return new ModelRoute(
                model.getName(),
                StringUtils.hasText(model.getProviderModel()) ? model.getProviderModel() : model.getName(),
                model.getProvider(),
                provider.getType(),
                providerMode,
                reason,
                copy(model.getCapabilities()),
                copy(model.getFallbackModels())
        );
    }

    private List<String> copy(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
