package com.medstock.service;

import com.medstock.entity.AiProviderConfig;
import com.medstock.repository.AiProviderConfigRepository;
import com.medstock.service.ai.AiProvider;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiProviderFactory {

    private final AiProviderConfigRepository aiProviderConfigRepository;
    private final Map<String, AiProvider> aiProviders;
    private final Environment environment;
    private final Map<String, AiProviderConfig> activeByUseCase = new ConcurrentHashMap<>();

    @PostConstruct
    public void preload() {
        reloadFromDatabase();
    }

    public void clearCache() {
        activeByUseCase.clear();
        reloadFromDatabase();
    }

    public void setActiveProvider(Long providerId) {
        aiProviderConfigRepository.findById(providerId).ifPresent(config -> {
            config.setActive(true);
            aiProviderConfigRepository.save(config);
            reloadFromDatabase();
        });
    }

    public Optional<AiProviderConfig> getActiveConfig(String useCase) {
        String normalizedUseCase = normalizeUseCase(useCase);
        AiProviderConfig cached = activeByUseCase.get(normalizedUseCase);
        if (cached != null) {
            return Optional.of(cached);
        }

        Optional<AiProviderConfig> fromDb = aiProviderConfigRepository
            .findFirstByUseCaseIgnoreCaseAndActiveTrueOrderByUpdatedAtDesc(normalizedUseCase)
            .or(() -> aiProviderConfigRepository.findFirstByUseCaseIgnoreCaseAndActiveTrueOrderByUpdatedAtDesc("DEFAULT"));

        fromDb.ifPresent(config -> activeByUseCase.put(normalizeUseCase(config.getUseCase()), config));
        return fromDb;
    }

    public Optional<AiProviderConfig> resolveRuntimeConfig(String useCase) {
        String selectedProvider = selectProviderFromEnv();
        String normalizedUseCase = normalizeUseCase(useCase);

        if (selectedProvider != null) {
            Optional<AiProviderConfig> byUseCase = aiProviderConfigRepository
                .findFirstByProviderKeyIgnoreCaseAndUseCaseIgnoreCaseOrderByUpdatedAtDesc(selectedProvider, normalizedUseCase);
            if (byUseCase.isPresent()) {
                return byUseCase;
            }

            Optional<AiProviderConfig> defaultByProvider = aiProviderConfigRepository
                .findFirstByProviderKeyIgnoreCaseAndUseCaseIgnoreCaseOrderByUpdatedAtDesc(selectedProvider, "DEFAULT");
            if (defaultByProvider.isPresent()) {
                return defaultByProvider;
            }

            AiProviderConfig synthetic = new AiProviderConfig();
            synthetic.setName(selectedProvider);
            synthetic.setProviderKey(selectedProvider);
            synthetic.setUseCase(normalizedUseCase);
            synthetic.setBaseUrl(defaultBaseUrl(selectedProvider));
            synthetic.setApiKeyEnvVar(defaultKeyEnvVar(selectedProvider));
            synthetic.setActive(true);
            return Optional.of(synthetic);
        }

        return getActiveConfig(normalizedUseCase)
            .or(() -> getActiveConfig("DEFAULT"));
    }

    public Optional<AiProvider> getProvider(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(aiProviders.get(providerKey.trim().toUpperCase(Locale.ROOT)));
    }

    public void reloadFromDatabase() {
        activeByUseCase.clear();
        List<AiProviderConfig> configs = aiProviderConfigRepository.findByActiveTrue();
        for (AiProviderConfig config : configs) {
            String useCase = normalizeUseCase(config.getUseCase());
            activeByUseCase.putIfAbsent(useCase, config);
        }
    }

    private String selectProviderFromEnv() {
        String forced = environment.getProperty("MEDSTOCK_AI_PROVIDER");
        if (forced != null && !forced.isBlank()) {
            String normalizedForced = forced.trim().toUpperCase(Locale.ROOT);
            if (isAvailableByEnv(normalizedForced)) {
                return normalizedForced;
            }
        }

        if (hasText(environment.getProperty("MEDSTOCK_GEMINI_API_KEY"))) {
            return "GEMINI";
        }
        if (hasText(environment.getProperty("MEDSTOCK_OPENAI_API_KEY"))) {
            return "OPENAI";
        }
        if (hasText(environment.getProperty("MEDSTOCK_CLAUDE_API_KEY"))) {
            return "CLAUDE";
        }

        String ollamaBase = environment.getProperty("MEDSTOCK_OLLAMA_BASE_URL");
        if (hasText(ollamaBase)) {
            return "OLLAMA";
        }
        return null;
    }

    private boolean isAvailableByEnv(String providerKey) {
        return switch (providerKey) {
            case "GEMINI" -> hasText(environment.getProperty("MEDSTOCK_GEMINI_API_KEY"));
            case "OPENAI" -> hasText(environment.getProperty("MEDSTOCK_OPENAI_API_KEY"));
            case "CLAUDE" -> hasText(environment.getProperty("MEDSTOCK_CLAUDE_API_KEY"));
            case "OLLAMA" -> true;
            default -> false;
        };
    }

    private String defaultBaseUrl(String providerKey) {
        return switch (providerKey) {
            case "GEMINI" -> "https://generativelanguage.googleapis.com";
            case "OPENAI" -> "https://api.openai.com";
            case "CLAUDE" -> "https://api.anthropic.com";
            case "OLLAMA" -> {
                String envBase = environment.getProperty("MEDSTOCK_OLLAMA_BASE_URL");
                yield hasText(envBase) ? envBase : "http://localhost:11434";
            }
            default -> "https://generativelanguage.googleapis.com";
        };
    }

    private String defaultKeyEnvVar(String providerKey) {
        return switch (providerKey) {
            case "GEMINI" -> "MEDSTOCK_GEMINI_API_KEY";
            case "OPENAI" -> "MEDSTOCK_OPENAI_API_KEY";
            case "CLAUDE" -> "MEDSTOCK_CLAUDE_API_KEY";
            default -> null;
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeUseCase(String useCase) {
        if (useCase == null || useCase.isBlank()) {
            return "DEFAULT";
        }
        return useCase.trim().toUpperCase(Locale.ROOT);
    }
}
