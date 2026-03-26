package com.medstock.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 60, message = "Username must be between 3 and 60 characters")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Username can contain only letters, numbers, and underscore")
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    @Size(max = 180)
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "Password must include at least one uppercase letter, one lowercase letter, and one number"
    )
    String password
) {
}
