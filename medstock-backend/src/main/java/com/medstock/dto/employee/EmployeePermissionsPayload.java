package com.medstock.dto.employee;

public record EmployeePermissionsPayload(
    Boolean canAdd,
    Boolean canEdit,
    Boolean canDelete,
    Boolean canViewFinance,
    Boolean canSell
) {
}
