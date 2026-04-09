package com.medstock.dto.admin;

import com.medstock.entity.User;
import java.time.LocalDateTime;

public record AdminUserRowResponse(
    Long id,
    Long storeId,
    String username,
    String email,
    String fullName,
    String phone,
    String role,
    Boolean isActive,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt
) {
    public static AdminUserRowResponse from(User user) {
        return new AdminUserRowResponse(
            user.getId(),
            user.getStoreId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            user.getRole(),
            user.getIsActive(),
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }
}
