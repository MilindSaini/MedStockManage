package com.medstock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.medstock.dto.ai.AiAutofillResponse;
import com.medstock.dto.ai.AiImageRequest;
import com.medstock.dto.ai.AiTextResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAutofillService {

    private final AiProviderService aiProviderService;

    public AiAutofillResponse autofill(byte[] imageBytes, String mimeType) {
        AiImageRequest request = new AiImageRequest(
            imageBytes,
            mimeType,
            List.of(
                "Extract medicine details from the image and respond as strict JSON only.",
                "Keys: name,genericName,category,manufacturer,skuCode,unit,mrp,purchasePrice,lowStockThreshold",
                "If unknown, keep empty string for text and 0 for numeric values."
            )
        );

        AiTextResponse response = aiProviderService.analyzeImage("MEDICINE_AUTOFILL", request);
        JsonNode payload = aiProviderService.parseJsonSafe(response.content());

        return new AiAutofillResponse(
            payload.path("name").asText(""),
            payload.path("genericName").asText(""),
            payload.path("category").asText(""),
            payload.path("manufacturer").asText(""),
            payload.path("skuCode").asText(""),
            payload.path("unit").asText("pcs"),
            payload.path("mrp").asDouble(0),
            payload.path("purchasePrice").asDouble(0),
            payload.path("lowStockThreshold").asInt(10),
            response.providerUsed(),
            response.success(),
            response.success() ? "Autofill generated" : (response.errorMessage() == null ? "Autofill failed" : response.errorMessage())
        );
    }
}
