package com.medstock.dto.auth;

import com.medstock.entity.User;
import com.medstock.security.RoleUtils;
import java.util.List;

public record AuthUserResponse(
    Long id,
    Long storeId,
    String username,
    String email,
    String fullName,
    String role,
    List<String> roles
) {
    public static AuthUserResponse from(User user) {
        List<String> roles = RoleUtils.parseRoles(user.getRole());
        return new AuthUserResponse(
            user.getId(),
            user.getStoreId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            RoleUtils.primaryRole(roles),
            roles
        );
    }
}
