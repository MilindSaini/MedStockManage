package com.medstock.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record UpdateAiProviderRequest(
    @NotBlank(message = "name is required")
    String name,
    @NotBlank(message = "baseUrl is required")
    String baseUrl,
    String apiKey,
    Boolean active
) {
}
