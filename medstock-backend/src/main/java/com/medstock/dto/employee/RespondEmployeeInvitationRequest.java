package com.medstock.dto.employee;

import jakarta.validation.constraints.NotNull;

public record RespondEmployeeInvitationRequest(
    @NotNull Boolean accept
) {
}
