package com.medstock.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class MedicineStatusUtil {

    private MedicineStatusUtil() {
    }

    public static AlertType computeStatus(LocalDate expiryDate, Integer currentStock, Integer threshold) {
        int safeCurrentStock = currentStock == null ? 0 : currentStock;
        int safeThreshold = threshold == null ? 0 : Math.max(threshold, 0);

        if (safeCurrentStock <= 0) {
            return AlertType.OUT_OF_STOCK;
        }

        if (expiryDate != null) {
            LocalDate today = LocalDate.now();
            if (expiryDate.isBefore(today)) {
                return AlertType.EXPIRED;
            }

            long daysToExpiry = ChronoUnit.DAYS.between(today, expiryDate);
            if (daysToExpiry <= 7) {
                return AlertType.CRITICAL;
            }
            if (daysToExpiry <= 30) {
                return AlertType.WARNING;
            }
        }

        if (safeCurrentStock <= safeThreshold) {
            return AlertType.LOW_STOCK;
        }

        return AlertType.OK;
    }
}
