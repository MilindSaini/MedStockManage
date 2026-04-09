package com.medstock.dto.ai;

public record AiTextResponse(
    String content,
    String providerUsed,
    boolean success,
    String errorMessage
) {
    public static AiTextResponse failure(String providerUsed, String errorMessage) {
        return new AiTextResponse(null, providerUsed, false, errorMessage);
    }
}
