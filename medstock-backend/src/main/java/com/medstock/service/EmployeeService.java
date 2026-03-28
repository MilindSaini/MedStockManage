package com.medstock.service;

import com.medstock.dto.employee.AddEmployeeRequest;
import com.medstock.dto.employee.EmployeeInvitationResponse;
import com.medstock.dto.employee.EmployeePermissionsPayload;
import com.medstock.dto.employee.EmployeeResponse;
import com.medstock.entity.EmployeeInvitation;
import com.medstock.entity.EmployeePermission;
import com.medstock.entity.Store;
import com.medstock.entity.User;
import com.medstock.repository.EmployeeInvitationRepository;
import com.medstock.repository.EmployeePermissionRepository;
import com.medstock.repository.StoreRepository;
import com.medstock.repository.UserRepository;
import com.medstock.security.RoleUtils;
import com.medstock.security.UserPrincipal;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final EmployeeInvitationRepository employeeInvitationRepository;
    private final EmployeePermissionRepository employeePermissionRepository;
    private final RefreshTokenSessionService refreshTokenSessionService;
    private final PermissionGuard permissionGuard;

    @Transactional
    public EmployeeResponse addEmployee(UserPrincipal principal, AddEmployeeRequest request) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertOwnerOrAdmin(principal);
        permissionGuard.assertCanAdd(principal, storeId);

        String username = normalizeRequired(request.username(), "Username is required");
        User invitedUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (invitedUser.getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot invite yourself");
        }

        if (!Boolean.TRUE.equals(invitedUser.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User account is disabled");
        }

        if (RoleUtils.hasRole(invitedUser.getRole(), "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin user cannot be invited as employee");
        }

        if (storeId.equals(invitedUser.getStoreId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already in your store");
        }

        if (employeeInvitationRepository.findByStoreIdAndInvitedUserIdAndStatus(storeId, invitedUser.getId(), "PENDING").isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already pending for this user");
        }

        LocalDateTime now = LocalDateTime.now();

        EmployeeInvitation invitation = new EmployeeInvitation();
        invitation.setStoreId(storeId);
        invitation.setOwnerUserId(principal.getId());
        invitation.setInvitedUserId(invitedUser.getId());
        invitation.setInvitedUsername(invitedUser.getUsername());
        invitation.setInvitedEmail(invitedUser.getEmail());
        invitation.setStatus("PENDING");
        invitation.setCanAdd(Boolean.TRUE.equals(request.permissions() != null ? request.permissions().canAdd() : null));
        invitation.setCanEdit(Boolean.TRUE.equals(request.permissions() != null ? request.permissions().canEdit() : null));
        invitation.setCanDelete(Boolean.TRUE.equals(request.permissions() != null ? request.permissions().canDelete() : null));
        invitation.setCanViewFinance(Boolean.TRUE.equals(request.permissions() != null ? request.permissions().canViewFinance() : null));
        invitation.setCanSell(Boolean.TRUE.equals(request.permissions() != null ? request.permissions().canSell() : null));
        invitation.setCreatedAt(now);
        invitation.setUpdatedAt(now);
        employeeInvitationRepository.save(invitation);

        return EmployeeResponse.from(invitedUser, null);
    }

    public List<EmployeeInvitationResponse> listPendingInvitations(UserPrincipal principal) {
        List<EmployeeInvitation> invitations = employeeInvitationRepository.findByInvitedUserIdAndStatusOrderByCreatedAtDesc(
            principal.getId(),
            "PENDING"
        );

        return invitations.stream()
            .map(invitation -> {
                Store store = storeRepository.findById(invitation.getStoreId()).orElse(null);
                String storeName = store != null ? store.getName() : null;
                return EmployeeInvitationResponse.from(invitation, storeName);
            })
            .toList();
    }

    @Transactional
    public void respondToInvitation(UserPrincipal principal, Long invitationId, boolean accept) {
        EmployeeInvitation invitation = employeeInvitationRepository.findById(invitationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (!"PENDING".equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invitation already resolved");
        }

        if (!invitation.getInvitedUserId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot respond to this invitation");
        }

        LocalDateTime now = LocalDateTime.now();

        if (accept) {
            User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            if (user.getStoreId() != null && !user.getStoreId().equals(invitation.getStoreId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You already belong to another store");
            }

            user.setStoreId(invitation.getStoreId());
            user.setRole(RoleUtils.serializeRoles(RoleUtils.addRole(user.getRole(), "EMPLOYEE")));
            user.setUpdatedAt(now);
            userRepository.save(user);

            EmployeePermission permission = employeePermissionRepository.findByStoreIdAndUserId(invitation.getStoreId(), user.getId())
                .orElseGet(() -> {
                    EmployeePermission created = new EmployeePermission();
                    created.setStoreId(invitation.getStoreId());
                    created.setUserId(user.getId());
                    created.setCreatedAt(now);
                    return created;
                });

            permission.setCanAdd(Boolean.TRUE.equals(invitation.getCanAdd()));
            permission.setCanEdit(Boolean.TRUE.equals(invitation.getCanEdit()));
            permission.setCanDelete(Boolean.TRUE.equals(invitation.getCanDelete()));
            permission.setCanViewFinance(Boolean.TRUE.equals(invitation.getCanViewFinance()));
            permission.setCanSell(Boolean.TRUE.equals(invitation.getCanSell()));
            permission.setUpdatedAt(now);
            employeePermissionRepository.save(permission);
        }

        invitation.setStatus(accept ? "ACCEPTED" : "DECLINED");
        invitation.setRespondedAt(now);
        invitation.setUpdatedAt(now);
        employeeInvitationRepository.save(invitation);
    }

    @Transactional
    public void removeEmployee(UserPrincipal principal, Long employeeUserId) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertOwnerOrAdmin(principal);
        permissionGuard.assertCanDelete(principal, storeId);

        User employee = userRepository.findById(employeeUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (!storeId.equals(employee.getStoreId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee does not belong to this store");
        }

        employee.setStoreId(null);
        employee.setRole(RoleUtils.serializeRoles(RoleUtils.addRole(employee.getRole(), "EMPLOYEE")));
        employee.setUpdatedAt(LocalDateTime.now());
        userRepository.save(employee);

        employeePermissionRepository.deleteByStoreIdAndUserId(storeId, employeeUserId);
        refreshTokenSessionService.revokeAllActiveSessionsForUser(employeeUserId);
    }

    @Transactional
    public EmployeeResponse updatePermissions(UserPrincipal principal, Long employeeUserId, EmployeePermissionsPayload payload) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertOwnerOrAdmin(principal);
        permissionGuard.assertCanEdit(principal, storeId);

        User employee = userRepository.findById(employeeUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (!storeId.equals(employee.getStoreId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Employee does not belong to this store");
        }

        EmployeePermission permission = employeePermissionRepository.findByStoreIdAndUserId(storeId, employeeUserId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permissions not found"));

        applyPermissions(permission, payload);
        permission.setUpdatedAt(LocalDateTime.now());
        permission = employeePermissionRepository.save(permission);

        return EmployeeResponse.from(employee, permission);
    }

    public List<EmployeeResponse> listEmployees(UserPrincipal principal) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertStoreAccess(principal, storeId);

        return userRepository.findByStoreIdAndIsActiveTrue(storeId).stream()
            .filter(user -> user.getId() != null && !user.getId().equals(principal.getId()))
            .map(user -> EmployeeResponse.from(
                user,
                employeePermissionRepository.findByStoreIdAndUserId(storeId, user.getId()).orElse(null)
            ))
            .toList();
    }

    public EmployeePermissionsPayload getMyPermissions(UserPrincipal principal) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertStoreAccess(principal, storeId);

        if (principal.getRoles().contains("OWNER") || principal.getRoles().contains("ADMIN")) {
            return new EmployeePermissionsPayload(true, true, true, true, true);
        }

        EmployeePermission permission = employeePermissionRepository.findByStoreIdAndUserId(storeId, principal.getId())
            .orElse(null);

        return new EmployeePermissionsPayload(
            permission != null && Boolean.TRUE.equals(permission.getCanAdd()),
            permission != null && Boolean.TRUE.equals(permission.getCanEdit()),
            permission != null && Boolean.TRUE.equals(permission.getCanDelete()),
            permission != null && Boolean.TRUE.equals(permission.getCanViewFinance()),
            permission != null && Boolean.TRUE.equals(permission.getCanSell())
        );
    }

    private void applyPermissions(EmployeePermission permission, EmployeePermissionsPayload payload) {
        permission.setCanAdd(Boolean.TRUE.equals(payload != null ? payload.canAdd() : null));
        permission.setCanEdit(Boolean.TRUE.equals(payload != null ? payload.canEdit() : null));
        permission.setCanDelete(Boolean.TRUE.equals(payload != null ? payload.canDelete() : null));
        permission.setCanViewFinance(Boolean.TRUE.equals(payload != null ? payload.canViewFinance() : null));
        permission.setCanSell(Boolean.TRUE.equals(payload != null ? payload.canSell() : null));
    }

    private Long requireStoreId(UserPrincipal principal) {
        if (principal.getStoreId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not attached to any store");
        }
        return principal.getStoreId();
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

}
