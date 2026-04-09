package com.medstock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medstock.dto.ai.AiImageRequest;
import com.medstock.dto.ai.AiTextRequest;
import com.medstock.dto.ai.AiTextResponse;
import com.medstock.entity.AiProviderConfig;
import com.medstock.service.ai.AiProvider;
import com.medstock.service.ai.AiProviderRuntimeContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiProviderService {

    private final AiProviderFactory aiProviderFactory;
    private final ObjectMapper objectMapper;

    public AiTextResponse generateText(AiTextRequest request) {
        return generateText("DEFAULT", request);
    }

    public AiTextResponse generateText(String useCase, AiTextRequest request) {
        try {
            Optional<AiProviderConfig> configOpt = aiProviderFactory.resolveRuntimeConfig(useCase);
            if (configOpt.isEmpty()) {
                return AiTextResponse.failure("NONE", "No active AI provider config");
            }

            AiProviderConfig config = configOpt.get();
            Optional<AiProvider> providerOpt = aiProviderFactory.getProvider(config.getProviderKey());
            if (providerOpt.isEmpty()) {
                return AiTextResponse.failure(config.getProviderKey(), "No provider bean found");
            }

            AiProviderRuntimeContext.set(config);
            AiTextResponse raw = providerOpt.get().generateText(request);
            return sanitize(raw);
        } catch (Exception exception) {
            return AiTextResponse.failure("UNKNOWN", exception.getMessage());
        } finally {
            AiProviderRuntimeContext.clear();
        }
    }

    public AiTextResponse analyzeImage(AiImageRequest request) {
        return analyzeImage("MEDICINE_AUTOFILL", request);
    }

    public AiTextResponse analyzeImage(String useCase, AiImageRequest request) {
        try {
            Optional<AiProviderConfig> configOpt = aiProviderFactory.resolveRuntimeConfig(useCase)
                .or(() -> aiProviderFactory.resolveRuntimeConfig("DEFAULT"));
            if (configOpt.isEmpty()) {
                return AiTextResponse.failure("NONE", "No active AI provider config");
            }

            AiProviderConfig config = configOpt.get();
            Optional<AiProvider> providerOpt = aiProviderFactory.getProvider(config.getProviderKey());
            if (providerOpt.isEmpty()) {
                return AiTextResponse.failure(config.getProviderKey(), "No provider bean found");
            }

            AiProviderRuntimeContext.set(config);
            AiTextResponse raw = providerOpt.get().analyzeImage(request);
            return sanitize(raw);
        } catch (Exception exception) {
            return AiTextResponse.failure("UNKNOWN", exception.getMessage());
        } finally {
            AiProviderRuntimeContext.clear();
        }
    }

    public JsonNode parseJsonSafe(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(stripMarkdownFences(rawContent));
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private AiTextResponse sanitize(AiTextResponse response) {
        if (response == null) {
            return AiTextResponse.failure("UNKNOWN", "AI provider returned null response");
        }

        String cleaned = stripMarkdownFences(response.content());
        return new AiTextResponse(cleaned, response.providerUsed(), response.success(), response.errorMessage());
    }

    private String stripMarkdownFences(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}
