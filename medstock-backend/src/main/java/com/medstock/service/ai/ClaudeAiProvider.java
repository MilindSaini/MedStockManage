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
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component("CLAUDE")
public class ClaudeAiProvider extends BaseHttpAiProvider implements AiProvider {

    public ClaudeAiProvider(ObjectMapper objectMapper, Environment environment) {
        super(objectMapper, environment);
    }

    @Override
    public AiTextResponse generateText(AiTextRequest request) {
        try {
            AiProviderConfig config = requiredConfig("CLAUDE");
            String apiKey = resolveApiKey(config);
            String model = config.getModel() == null || config.getModel().isBlank() ? "claude-3-5-sonnet-latest" : config.getModel();
            String url = (config.getBaseUrl() == null || config.getBaseUrl().isBlank()
                ? "https://api.anthropic.com"
                : config.getBaseUrl()) + "/v1/messages";

            JsonNode json = postJson(
                url,
                Map.of(
                    "x-api-key", apiKey,
                    "anthropic-version", "2023-06-01"
                ),
                Map.of(
                    "model", model,
                    "max_tokens", 1024,
                    "system", request.systemPrompt(),
                    "messages", List.of(Map.of("role", "user", "content", request.userPrompt()))
                )
            );

            String content = json.path("content").path(0).path("text").asText(null);
            return new AiTextResponse(content, "CLAUDE", content != null, content == null ? "Empty response" : null);
        } catch (Exception exception) {
            return failure("CLAUDE", exception);
        }
    }

    @Override
    public AiTextResponse analyzeImage(AiImageRequest request) {
        try {
            AiProviderConfig config = requiredConfig("CLAUDE");
            String apiKey = resolveApiKey(config);
            String model = config.getModel() == null || config.getModel().isBlank() ? "claude-3-5-sonnet-latest" : config.getModel();
            String url = (config.getBaseUrl() == null || config.getBaseUrl().isBlank()
                ? "https://api.anthropic.com"
                : config.getBaseUrl()) + "/v1/messages";
            String prompt = String.join("\n", request.prompts() == null ? List.of() : request.prompts());

            JsonNode json = postJson(
                url,
                Map.of(
                    "x-api-key", apiKey,
                    "anthropic-version", "2023-06-01"
                ),
                Map.of(
                    "model", model,
                    "max_tokens", 1024,
                    "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                            Map.of(
                                "type", "image",
                                "source", Map.of(
                                    "type", "base64",
                                    "media_type", request.mimeType(),
                                    "data", Base64.getEncoder().encodeToString(request.imageBytes())
                                )
                            ),
                            Map.of("type", "text", "text", prompt)
                        )
                    ))
                )
            );

            String content = json.path("content").path(0).path("text").asText(null);
            return new AiTextResponse(content, "CLAUDE", content != null, content == null ? "Empty response" : null);
        } catch (Exception exception) {
            return failure("CLAUDE", exception);
        }
    }
}
