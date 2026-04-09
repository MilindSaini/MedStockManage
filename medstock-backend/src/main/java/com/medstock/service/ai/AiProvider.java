package com.medstock.service.ai;

import com.medstock.dto.ai.AiImageRequest;
import com.medstock.dto.ai.AiTextRequest;
import com.medstock.dto.ai.AiTextResponse;

public interface AiProvider {

    AiTextResponse generateText(AiTextRequest request);

    AiTextResponse analyzeImage(AiImageRequest request);
}
