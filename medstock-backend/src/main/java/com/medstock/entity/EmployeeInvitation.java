package com.medstock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "employee_invitations")
public class EmployeeInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "invited_user_id", nullable = false)
    private Long invitedUserId;

    @Column(name = "invited_username", nullable = false)
    private String invitedUsername;

    @Column(name = "invited_email", nullable = false)
    private String invitedEmail;

    @Column(nullable = false)
    private String status;

    @Column(name = "can_add", nullable = false)
    private Boolean canAdd = Boolean.FALSE;

    @Column(name = "can_edit", nullable = false)
    private Boolean canEdit = Boolean.FALSE;

    @Column(name = "can_delete", nullable = false)
    private Boolean canDelete = Boolean.FALSE;

    @Column(name = "can_view_finance", nullable = false)
    private Boolean canViewFinance = Boolean.FALSE;

    @Column(name = "can_sell", nullable = false)
    private Boolean canSell = Boolean.FALSE;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
