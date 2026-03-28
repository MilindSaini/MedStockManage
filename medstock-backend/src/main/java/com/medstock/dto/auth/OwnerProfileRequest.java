package com.medstock.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerProfileRequest(
    @NotBlank(message = "Store name is required")
    @Size(max = 150)
    String storeName,

    @NotBlank(message = "Store address is required")
    @Size(max = 255)
    String storeAddress
) {
}
