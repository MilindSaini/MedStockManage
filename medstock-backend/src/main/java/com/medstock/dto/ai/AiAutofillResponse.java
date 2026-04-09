package com.medstock.dto.ai;

public record AiAutofillResponse(
    String name,
    String genericName,
    String category,
    String manufacturer,
    String skuCode,
    String unit,
    Double mrp,
    Double purchasePrice,
    Integer lowStockThreshold,
    String providerUsed,
    boolean success,
    String message
) {
}
