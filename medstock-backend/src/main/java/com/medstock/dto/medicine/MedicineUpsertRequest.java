package com.medstock.dto.medicine;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicineUpsertRequest(
    @NotBlank String name,
    String genericName,
    String category,
    String manufacturer,
    String skuCode,
    String unit,
    @NotNull @Min(0) BigDecimal mrp,
    @NotNull @Min(0) BigDecimal purchasePrice,
    @NotNull @Min(0) Integer quantityAvailable,
    @NotNull @Min(0) Integer quantitySold,
    @NotNull @Min(0) Integer lowStockThreshold,
    LocalDate expiryDate
) {
}
