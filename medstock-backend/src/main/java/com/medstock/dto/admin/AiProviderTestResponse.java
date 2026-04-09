package com.medstock.dto.admin;

public record AiProviderTestResponse(
    Long id,
    String name,
    boolean ok,
    int statusCode,
    String message
) {
}
