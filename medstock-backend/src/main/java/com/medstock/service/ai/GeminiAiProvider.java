package com.medstock.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medstock.dto.ai.AiImageRequest;
import com.medstock.dto.ai.AiTextRequest;
import com.medstock.dto.ai.AiTextResponse;
import com.medstock.entity.AiProviderConfig;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component("GEMINI")
public class GeminiAiProvider extends BaseHttpAiProvider implements AiProvider {

    public GeminiAiProvider(ObjectMapper objectMapper, Environment environment) {
        super(objectMapper, environment);
    }

    @Override
    public AiTextResponse generateText(AiTextRequest request) {
        try {
            AiProviderConfig config = requiredConfig("GEMINI");
            String apiKey = resolveApiKey(config);
            JsonNode json = postWithModelFallback(
                config,
                apiKey,
                Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", request.systemPrompt() + "\n\n" + request.userPrompt())))),
                    "generationConfig", Map.of("temperature", request.temperature() == null ? 0.2 : request.temperature())
                )
            );
            String content = json.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
            return new AiTextResponse(content, "GEMINI", content != null, content == null ? "Empty response" : null);
        } catch (Exception exception) {
            return failure("GEMINI", exception);
        }
    }

    @Override
    public AiTextResponse analyzeImage(AiImageRequest request) {
        try {
            AiProviderConfig config = requiredConfig("GEMINI");
            String apiKey = resolveApiKey(config);
            String prompt = String.join("\n", request.prompts() == null ? List.of() : request.prompts());
            JsonNode json = postWithModelFallback(
                config,
                apiKey,
                Map.of(
                    "contents", List.of(Map.of("parts", List.of(
                        Map.of("text", prompt),
                        Map.of("inlineData", Map.of(
                            "mimeType", request.mimeType(),
                            "data", Base64.getEncoder().encodeToString(request.imageBytes())
                        ))
                    ))),
                    "generationConfig", Map.of("temperature", 0.1)
                )
            );
            String content = json.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
            return new AiTextResponse(content, "GEMINI", content != null, content == null ? "Empty response" : null);
        } catch (Exception exception) {
            return failure("GEMINI", exception);
        }
    }

    private String resolveModel(AiProviderConfig config) {
        if (config.getModel() != null && !config.getModel().isBlank()) {
            return config.getModel().trim();
        }

        String fromEnv = environment.getProperty("MEDSTOCK_GEMINI_MODEL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        return "gemini-2.5-flash";
    }

    private JsonNode postWithModelFallback(AiProviderConfig config, String apiKey, Map<String, Object> payload) throws Exception {
        List<String> candidates = List.of(
            resolveModel(config),
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-1.5-flash"
        );

        Exception last = null;
        for (String model : candidates.stream().filter(m -> m != null && !m.isBlank()).distinct().toList()) {
            try {
                String url = resolveBaseUrl(config)
                    + "/v1beta/models/" + model.trim() + ":generateContent?key=" + apiKey;
                return postJson(url, Map.of(), payload);
            } catch (Exception exception) {
                last = exception;
                if (!isModelUnavailableError(exception.getMessage())) {
                    throw exception;
                }
            }
        }

        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("No Gemini model candidates available");
    }

    private boolean isModelUnavailableError(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("http 404")
            && lower.contains("model")
            && (lower.contains("no longer available") || lower.contains("not found") || lower.contains("unsupported"));
    }

    private String resolveBaseUrl(AiProviderConfig config) {
        String baseUrl = (config.getBaseUrl() == null || config.getBaseUrl().isBlank())
            ? "https://generativelanguage.googleapis.com"
            : config.getBaseUrl().trim();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
