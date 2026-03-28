package com.medstock.service;

import com.medstock.entity.EmployeePermission;
import com.medstock.repository.EmployeePermissionRepository;
import com.medstock.security.RoleUtils;
import com.medstock.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PermissionGuard {

    private final EmployeePermissionRepository employeePermissionRepository;

    public void assertCanAdd(UserPrincipal principal, Long storeId) {
        assertStoreAccess(principal, storeId);
        if (isOwnerOrAdmin(principal)) {
            return;
        }
        EmployeePermission permission = getEmployeePermission(principal, storeId);
        if (!Boolean.TRUE.equals(permission.getCanAdd())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Add permission is required");
        }
    }

    public void assertCanEdit(UserPrincipal principal, Long storeId) {
        assertStoreAccess(principal, storeId);
        if (isOwnerOrAdmin(principal)) {
            return;
        }
        EmployeePermission permission = getEmployeePermission(principal, storeId);
        if (!Boolean.TRUE.equals(permission.getCanEdit())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Edit permission is required");
        }
    }

    public void assertCanDelete(UserPrincipal principal, Long storeId) {
        assertStoreAccess(principal, storeId);
        if (isOwnerOrAdmin(principal)) {
            return;
        }
        EmployeePermission permission = getEmployeePermission(principal, storeId);
        if (!Boolean.TRUE.equals(permission.getCanDelete())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Delete permission is required");
        }
    }

    public void assertCanViewFinance(UserPrincipal principal, Long storeId) {
        assertStoreAccess(principal, storeId);
        if (isOwnerOrAdmin(principal)) {
            return;
        }
        EmployeePermission permission = getEmployeePermission(principal, storeId);
        if (!Boolean.TRUE.equals(permission.getCanViewFinance())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Finance permission is required");
        }
    }

    public void assertCanSell(UserPrincipal principal, Long storeId) {
        assertStoreAccess(principal, storeId);
        if (isOwnerOrAdmin(principal)) {
            return;
        }
        EmployeePermission permission = getEmployeePermission(principal, storeId);
        if (!Boolean.TRUE.equals(permission.getCanSell())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sell permission is required");
        }
    }

    public void assertStoreAccess(UserPrincipal principal, Long storeId) {
        if (storeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Store is required");
        }
        if (RoleUtils.hasRole(principal.getUser().getRole(), "ADMIN")) {
            return;
        }
        if (principal.getStoreId() == null || !principal.getStoreId().equals(storeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-store access denied");
        }
    }

    public void assertOwnerOrAdmin(UserPrincipal principal) {
        if (!isOwnerOrAdmin(principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Owner role required");
        }
    }

    private EmployeePermission getEmployeePermission(UserPrincipal principal, Long storeId) {
        return employeePermissionRepository.findByStoreIdAndUserId(storeId, principal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee permissions not configured"));
    }

    private boolean isOwnerOrAdmin(UserPrincipal principal) {
        return RoleUtils.hasRole(principal.getUser().getRole(), "OWNER")
            || RoleUtils.hasRole(principal.getUser().getRole(), "ADMIN");
    }
}
