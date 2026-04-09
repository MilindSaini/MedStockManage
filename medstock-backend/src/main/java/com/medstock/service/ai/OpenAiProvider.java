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

@Component("OPENAI")
public class OpenAiProvider extends BaseHttpAiProvider implements AiProvider {

    public OpenAiProvider(ObjectMapper objectMapper, Environment environment) {
        super(objectMapper, environment);
    }

    @Override
    public AiTextResponse generateText(AiTextRequest request) {
        try {
            AiProviderConfig config = requiredConfig("OPENAI");
            String apiKey = resolveApiKey(config);
            String model = config.getModel() == null || config.getModel().isBlank() ? "gpt-4o-mini" : config.getModel();
            String url = (config.getBaseUrl() == null || config.getBaseUrl().isBlank()
                ? "https://api.openai.com"
                : config.getBaseUrl()) + "/v1/chat/completions";

            JsonNode json = postJson(
                url,
                Map.of("Authorization", "Bearer " + apiKey),
                Map.of(
                    "model", model,
                    "temperature", request.temperature() == null ? 0.2 : request.temperature(),
                    "messages", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())
                    )
                )
            );

            String content = json.path("choices").path(0).path("message").path("content").asText(null);
            return new AiTextResponse(content, "OPENAI", content != null, content == null ? "Empty response" : null);
        } catch (Exception exception) {
            return failure("OPENAI", exception);
        }
    }

    @Override
    public AiTextResponse analyzeImage(AiImageRequest request) {
        try {
            AiProviderConfig config = requiredConfig("OPENAI");
            String apiKey = resolveApiKey(config);
            String model = config.getModel() == null || config.getModel().isBlank() ? "gpt-4o-mini" : config.getModel();
            String url = (config.getBaseUrl() == null || config.getBaseUrl().isBlank()
                ? "https://api.openai.com"
                : config.getBaseUrl()) + "/v1/chat/completions";
            String prompt = String.join("\n", request.prompts() == null ? List.of() : request.prompts());

            JsonNode json = postJson(
                url,
                Map.of("Authorization", "Bearer " + apiKey),
                Map.of(
                    "model", model,
                    "messages", List.of(
                        Map.of(
                            "role", "user",
                            "content", List.of(
                                Map.of("type", "text", "text", prompt),
                                Map.of(
                                    "type", "image_url",
                                    "image_url", Map.of(
                                        "url",
                                        "data:" + request.mimeType() + ";base64," + Base64.getEncoder().encodeToString(request.imageBytes())
                                    )
                                )
                            )
                        )
                    )
                )
            );

            String content = json.path("choices").path(0).path("message").path("content").asText(null);
            return new AiTextResponse(content, "OPENAI", content != null, content == null ? "Empty response" : null);
        } catch (Exception exception) {
            return failure("OPENAI", exception);
        }
    }
}
