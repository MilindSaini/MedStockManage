package com.medstock.dto.auth;

import com.medstock.entity.User;
import com.medstock.security.RoleUtils;
import java.util.List;

public record AuthUserResponse(
    Long id,
    Long storeId,
    String storeName,
    String username,
    String email,
    String fullName,
    String phone,
    Boolean phoneVerified,
    String role,
    List<String> roles
) {
    public static AuthUserResponse from(User user) {
        return from(user, null);
    }

    public static AuthUserResponse from(User user, String storeName) {
        List<String> roles = RoleUtils.parseRoles(user.getRole());
        return new AuthUserResponse(
            user.getId(),
            user.getStoreId(),
            storeName,
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            user.getPhoneVerified(),
            RoleUtils.primaryRole(roles),
            roles
        );
    }
}
