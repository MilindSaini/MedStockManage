package com.medstock.dto.employee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdateEmployeePermissionsRequest(
    @NotNull @Valid EmployeePermissionsPayload permissions
) {
}
