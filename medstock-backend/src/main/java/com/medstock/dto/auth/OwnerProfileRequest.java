package com.medstock.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerProfileRequest(
    @NotBlank(message = "Store name is required")
    @Size(max = 150)
    String storeName,

    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    String fullName,

    @NotBlank(message = "Phone number is required")
    @Size(max = 30)
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,30}$", message = "Invalid phone number")
    String phone
) {
}
