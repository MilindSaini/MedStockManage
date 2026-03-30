package com.medstock.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateStoreAlertScheduleRequest(
    @NotBlank(message = "Expiry alert time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Expiry alert time must be HH:mm")
    String expiryAlertTime,

    @NotBlank(message = "Low stock alert time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Low stock alert time must be HH:mm")
    String lowStockAlertTime,

    @NotBlank(message = "Out of stock alert time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Out of stock alert time must be HH:mm")
    String outOfStockAlertTime,

    @NotBlank(message = "Batch promotion time is required")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Batch promotion time must be HH:mm")
    String batchPromotionTime
) {
}
