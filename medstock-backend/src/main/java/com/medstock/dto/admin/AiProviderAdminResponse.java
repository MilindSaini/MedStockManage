package com.medstock.dto.admin;

import com.medstock.entity.AiProviderConfig;
import java.time.LocalDateTime;

public record AiProviderAdminResponse(
    Long id,
    String name,
    String baseUrl,
    Boolean active,
    String apiKeyMasked,
    String lastTestStatus,
    LocalDateTime lastTestedAt,
    LocalDateTime updatedAt
) {
    public static AiProviderAdminResponse from(AiProviderConfig config) {
        return new AiProviderAdminResponse(
            config.getId(),
            config.getName(),
            config.getBaseUrl(),
            config.getActive(),
            mask(config.getApiKey()),
            config.getLastTestStatus(),
            config.getLastTestedAt(),
            config.getUpdatedAt()
        );
    }

    private static String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        if (apiKey.length() <= 6) {
            return "***";
        }
        return apiKey.substring(0, 3) + "***" + apiKey.substring(apiKey.length() - 3);
    }
}
