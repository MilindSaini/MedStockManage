package com.medstock.dto.employee;

import com.medstock.entity.EmployeePermission;
import com.medstock.entity.User;

public record EmployeeResponse(
    Long id,
    String username,
    String email,
    String fullName,
    String phone,
    Boolean isActive,
    EmployeePermissionsPayload permissions
) {
    public static EmployeeResponse from(User user, EmployeePermission permission) {
        EmployeePermissionsPayload payload = new EmployeePermissionsPayload(
            permission != null ? permission.getCanAdd() : Boolean.FALSE,
            permission != null ? permission.getCanEdit() : Boolean.FALSE,
            permission != null ? permission.getCanDelete() : Boolean.FALSE,
            permission != null ? permission.getCanViewFinance() : Boolean.FALSE,
            permission != null ? permission.getCanSell() : Boolean.FALSE
        );

        return new EmployeeResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            user.getIsActive(),
            payload
        );
    }
}
