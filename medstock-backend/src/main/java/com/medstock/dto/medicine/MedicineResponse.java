package com.medstock.dto.medicine;

import com.medstock.entity.Medicine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MedicineResponse(
    Long id,
    Long storeId,
    String name,
    String genericName,
    String category,
    String manufacturer,
    String skuCode,
    String unit,
    BigDecimal mrp,
    BigDecimal purchasePrice,
    Integer quantityAvailable,
    Integer quantitySold,
    Integer lowStockThreshold,
    Integer currentStock,
    LocalDate expiryDate,
    Long activeBatchId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static MedicineResponse from(Medicine medicine) {
        return new MedicineResponse(
            medicine.getId(),
            medicine.getStoreId(),
            medicine.getName(),
            medicine.getGenericName(),
            medicine.getCategory(),
            medicine.getManufacturer(),
            medicine.getSkuCode(),
            medicine.getUnit(),
            medicine.getMrp(),
            medicine.getPurchasePrice(),
            medicine.getQuantityAvailable(),
            medicine.getQuantitySold(),
            medicine.getLowStockThreshold(),
            medicine.getCurrentStock(),
            medicine.getExpiryDate(),
            medicine.getActiveBatchId(),
            medicine.getCreatedAt(),
            medicine.getUpdatedAt()
        );
    }
}
