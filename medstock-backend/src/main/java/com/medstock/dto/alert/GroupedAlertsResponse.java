package com.medstock.dto.alert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record GroupedAlertsResponse(
    Map<String, List<AlertItemResponse>> groups,
    LocalDateTime generatedAt
) {
}
