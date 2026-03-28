package com.medstock.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddEmployeeRequest(
    @NotBlank @Size(min = 3, max = 60) String username,
    EmployeePermissionsPayload permissions
) {
}
