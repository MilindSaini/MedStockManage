package com.medstock.dto.ai;

public record AiTextRequest(
    String systemPrompt,
    String userPrompt,
    String responseFormat,
    Double temperature
) {
}
