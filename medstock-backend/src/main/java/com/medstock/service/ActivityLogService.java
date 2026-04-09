package com.medstock.service;

import com.medstock.entity.ActivityLog;
import com.medstock.repository.ActivityLogRepository;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public void log(
        Long userId,
        Long storeId,
        String action,
        String entityType,
        Long entityId,
        Map<String, Object> metadata
    ) {
        if (action == null || action.isBlank()) {
            return;
        }

        ActivityLog activityLog = new ActivityLog();
        activityLog.setUserId(userId);
        activityLog.setStoreId(storeId);
        activityLog.setAction(action.trim());
        activityLog.setEntityType(entityType);
        activityLog.setEntityId(entityId);
        activityLog.setDetails(metadata == null ? Map.of() : metadata);
        activityLog.setCreatedAt(LocalDateTime.now());
        activityLogRepository.save(activityLog);
    }
}
