package com.medstock.dto.admin;

import com.medstock.entity.ActivityLog;
import java.time.LocalDateTime;
import java.util.Map;

public record AdminActivityRowResponse(
    Long id,
    Long storeId,
    Long userId,
    String action,
    String entityType,
    Long entityId,
    Map<String, Object> metadata,
    LocalDateTime createdAt
) {
    public static AdminActivityRowResponse from(ActivityLog log) {
        return new AdminActivityRowResponse(
            log.getId(),
            log.getStoreId(),
            log.getUserId(),
            log.getAction(),
            log.getEntityType(),
            log.getEntityId(),
            log.getDetails(),
            log.getCreatedAt()
        );
    }
}
