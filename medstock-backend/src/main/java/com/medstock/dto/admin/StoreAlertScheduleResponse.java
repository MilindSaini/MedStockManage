package com.medstock.dto.admin;

import com.medstock.entity.Store;
import java.time.LocalTime;

public record StoreAlertScheduleResponse(
    Long storeId,
    String storeName,
    String address,
    String subscriptionStatus,
    String expiryAlertTime,
    String lowStockAlertTime,
    String outOfStockAlertTime,
    String batchPromotionTime
) {

    public static StoreAlertScheduleResponse from(Store store) {
        return new StoreAlertScheduleResponse(
            store.getId(),
            store.getName(),
            store.getAddress(),
            store.getSubscriptionStatus(),
            formatTime(store.getExpiryAlertTime(), LocalTime.of(8, 0)),
            formatTime(store.getLowStockAlertTime(), LocalTime.of(8, 30)),
            formatTime(store.getOutOfStockAlertTime(), LocalTime.of(9, 0)),
            formatTime(store.getBatchPromotionTime(), LocalTime.of(6, 0))
        );
    }

    private static String formatTime(LocalTime value, LocalTime fallback) {
        LocalTime safe = value == null ? fallback : value;
        return String.format("%02d:%02d", safe.getHour(), safe.getMinute());
    }
}
