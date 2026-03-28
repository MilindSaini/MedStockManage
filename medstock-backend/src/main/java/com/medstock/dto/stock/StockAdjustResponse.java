package com.medstock.dto.stock;

public record StockAdjustResponse(
    Long medicineId,
    Integer delta,
    Integer currentStock,
    Integer quantityAvailable,
    Integer quantitySold,
    String transactionType,
    String message
) {
}
