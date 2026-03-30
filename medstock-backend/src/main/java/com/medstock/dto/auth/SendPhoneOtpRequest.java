package com.medstock.dto.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendPhoneOtpRequest(
    @Size(max = 30, message = "Phone number must be at most 30 characters")
    @Pattern(regexp = "^[0-9+()\\-\\s]*$", message = "Invalid phone number")
    String phone
) {
}
