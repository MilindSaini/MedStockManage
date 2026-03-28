package com.medstock.dto.employee;

import com.medstock.entity.EmployeeInvitation;
import java.time.LocalDateTime;

public record EmployeeInvitationResponse(
    Long id,
    Long storeId,
    String storeName,
    Long ownerUserId,
    String invitedUsername,
    String invitedEmail,
    EmployeePermissionsPayload permissions,
    String status,
    LocalDateTime createdAt
) {
    public static EmployeeInvitationResponse from(EmployeeInvitation invitation, String storeName) {
        return new EmployeeInvitationResponse(
            invitation.getId(),
            invitation.getStoreId(),
            storeName,
            invitation.getOwnerUserId(),
            invitation.getInvitedUsername(),
            invitation.getInvitedEmail(),
            new EmployeePermissionsPayload(
                Boolean.TRUE.equals(invitation.getCanAdd()),
                Boolean.TRUE.equals(invitation.getCanEdit()),
                Boolean.TRUE.equals(invitation.getCanDelete()),
                Boolean.TRUE.equals(invitation.getCanViewFinance()),
                Boolean.TRUE.equals(invitation.getCanSell())
            ),
            invitation.getStatus(),
            invitation.getCreatedAt()
        );
    }
}
