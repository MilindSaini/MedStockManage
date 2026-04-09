package com.medstock.dto.ai;

import java.util.List;

public record AiImageRequest(
    byte[] imageBytes,
    String mimeType,
    List<String> prompts
) {
}
