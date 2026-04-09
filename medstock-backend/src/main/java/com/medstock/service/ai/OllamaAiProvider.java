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

@Component("OLLAMA")
public class OllamaAiProvider extends BaseHttpAiProvider implements AiProvider {

    public OllamaAiProvider(ObjectMapper objectMapper, Environment environment) {
        super(objectMapper, environment);
    }

    @Override
    public AiTextResponse generateText(AiTextRequest request) {
        try {
            AiProviderConfig config = requiredConfig("OLLAMA");
            String model = config.getModel() == null || config.getModel().isBlank() ? "llama3.1" : config.getModel();
            String url = (config.getBaseUrl() == null || config.getBaseUrl().isBlank()
                ? "http://localhost:11434"
                : config.getBaseUrl()) + "/api/generate";

            JsonNode json = postJson(
                url,
                Map.of(),
                Map.of(
                    "model", model,
                    "prompt", request.systemPrompt() + "\n\n" + request.userPrompt(),
                    "stream", false,
                    "options", Map.of("temperature", request.temperature() == null ? 0.2 : request.temperature())
                )
            );

            String content = json.path("response").asText(null);
            return new AiTextResponse(content, "OLLAMA", content != null, content == null ? "Empty response" : null);
        } catch (Exception exception) {
            return failure("OLLAMA", exception);
        }
    }

    @Override
    public AiTextResponse analyzeImage(AiImageRequest request) {
        try {
            AiProviderConfig config = requiredConfig("OLLAMA");
            String model = config.getModel() == null || config.getModel().isBlank() ? "llava" : config.getModel();
            String url = (config.getBaseUrl() == null || config.getBaseUrl().isBlank()
                ? "http://localhost:11434"
                : config.getBaseUrl()) + "/api/generate";
            String prompt = String.join("\n", request.prompts() == null ? List.of() : request.prompts());

            JsonNode json = postJson(
                url,
                Map.of(),
                Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false,
                    "images", List.of(Base64.getEncoder().encodeToString(request.imageBytes()))
                )
            );

            String content = json.path("response").asText(null);
            return new AiTextResponse(content, "OLLAMA", content != null, content == null ? "Empty response" : null);
        } catch (Exception exception) {
            return failure("OLLAMA", exception);
        }
    }
}
