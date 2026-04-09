package com.medstock.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medstock.dto.ai.AiTextResponse;
import com.medstock.entity.AiProviderConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.env.Environment;

abstract class BaseHttpAiProvider {

    protected final ObjectMapper objectMapper;
    protected final Environment environment;
    private final HttpClient httpClient;

    protected BaseHttpAiProvider(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    protected AiProviderConfig requiredConfig(String providerName) {
        AiProviderConfig config = AiProviderRuntimeContext.get();
        if (config == null) {
            throw new IllegalStateException("No active AI provider config for " + providerName);
        }
        return config;
    }

    protected String resolveApiKey(AiProviderConfig config) {
        String envVar = config.getApiKeyEnvVar();
        if (envVar == null || envVar.isBlank()) {
            envVar = defaultApiKeyEnvVar(config.getProviderKey());
        }
        if (envVar == null || envVar.isBlank()) {
            return null;
        }
        return environment.getProperty(envVar);
    }

    private String defaultApiKeyEnvVar(String providerKey) {
        if (providerKey == null || providerKey.isBlank()) {
            return null;
        }

        return switch (providerKey.trim().toUpperCase(Locale.ROOT)) {
            case "GEMINI" -> "MEDSTOCK_GEMINI_API_KEY";
            case "OPENAI" -> "MEDSTOCK_OPENAI_API_KEY";
            case "CLAUDE" -> "MEDSTOCK_CLAUDE_API_KEY";
            default -> null;
        };
    }

    protected JsonNode postJson(String url, Map<String, String> headers, Object payload) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));

        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getValue() != null && !header.getValue().isBlank()) {
                builder.header(header.getKey(), header.getValue());
            }
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException(buildProviderErrorMessage(response.statusCode(), response.body()));
        }
        return objectMapper.readTree(response.body());
    }

    private String buildProviderErrorMessage(int statusCode, String body) {
        String normalized = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (statusCode == 429 || lower.contains("quota") || lower.contains("rate limit")) {
            return "AI provider quota exceeded or rate limit reached (HTTP 429). Please retry later or check provider billing/quota.";
        }

        if (statusCode == 401 || statusCode == 403) {
            return "AI provider authentication failed (HTTP " + statusCode + "). Please check API key and provider permissions.";
        }

        if (statusCode >= 500) {
            return "AI provider is temporarily unavailable (HTTP " + statusCode + "). Please try again in a few minutes.";
        }

        if (normalized.length() > 160) {
            normalized = normalized.substring(0, 160) + "...";
        }

        return "AI provider request failed (HTTP " + statusCode + ")"
            + (normalized.isBlank() ? "" : ": " + normalized);
    }

    protected AiTextResponse failure(String provider, Exception exception) {
        String message = exception.getMessage() == null ? "AI provider error" : exception.getMessage();
        return AiTextResponse.failure(provider, message);
    }
}
