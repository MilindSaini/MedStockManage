package com.medstock.dto.admin;

import com.medstock.entity.Store;
import java.time.LocalDateTime;

public record AdminStoreRowResponse(
    Long id,
    String name,
    String address,
    Long ownerUserId,
    String subscriptionStatus,
    LocalDateTime trialEndsAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AdminStoreRowResponse from(Store store) {
        return new AdminStoreRowResponse(
            store.getId(),
            store.getName(),
            store.getAddress(),
            store.getOwnerUserId(),
            store.getSubscriptionStatus(),
            store.getTrialEndsAt(),
            store.getCreatedAt(),
            store.getUpdatedAt()
        );
    }
}
