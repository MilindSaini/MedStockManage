package com.medstock.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
    @NotBlank(message = "Username or email is required")
    String identifier,

    @NotBlank(message = "Password is required")
    String password
) {
}
