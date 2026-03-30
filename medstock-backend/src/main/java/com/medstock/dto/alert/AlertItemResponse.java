package com.medstock.dto.alert;

import com.medstock.entity.Medicine;
import com.medstock.service.AlertType;
import java.time.LocalDate;

public record AlertItemResponse(
    Long id,
    String name,
    Integer currentStock,
    Integer lowStockThreshold,
    LocalDate expiryDate,
    String status
) {

    public static AlertItemResponse from(Medicine medicine, AlertType status) {
        return new AlertItemResponse(
            medicine.getId(),
            medicine.getName(),
            medicine.getCurrentStock(),
            medicine.getLowStockThreshold(),
            medicine.getExpiryDate(),
            status.name()
        );
    }
}
