package com.medstock.dto.stock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(
    @NotNull Long medicineId,
    @NotNull Integer delta,
    @NotBlank String transactionType,
    String notes
) {
}
