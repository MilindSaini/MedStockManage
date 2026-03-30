package com.medstock.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyPhoneOtpRequest(
    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 10, message = "Invalid OTP")
    @Pattern(regexp = "^[0-9]+$", message = "OTP must be numeric")
    String otp
) {
}
