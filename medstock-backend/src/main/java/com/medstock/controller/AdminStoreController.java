package com.medstock.controller;

import com.medstock.dto.admin.StoreAlertScheduleResponse;
import com.medstock.dto.admin.UpdateStoreAlertScheduleRequest;
import com.medstock.entity.Store;
import com.medstock.security.UserPrincipal;
import com.medstock.repository.StoreRepository;
import com.medstock.service.ActivityLogService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreController {

    private final StoreRepository storeRepository;
    private final ActivityLogService activityLogService;

    @GetMapping("/schedules")
    public ResponseEntity<List<StoreAlertScheduleResponse>> listSchedules(Authentication authentication) {
        requirePrincipal(authentication);
        List<StoreAlertScheduleResponse> rows = storeRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(Store::getId))
            .map(StoreAlertScheduleResponse::from)
            .toList();
        return ResponseEntity.ok(rows);
    }

    @PutMapping("/{storeId}/schedules")
    public ResponseEntity<StoreAlertScheduleResponse> updateSchedule(
        @PathVariable Long storeId,
        @Valid @RequestBody UpdateStoreAlertScheduleRequest request,
        Authentication authentication
    ) {
        UserPrincipal principal = requirePrincipal(authentication);

        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found"));

        store.setExpiryAlertTime(LocalTime.parse(request.expiryAlertTime()));
        store.setLowStockAlertTime(LocalTime.parse(request.lowStockAlertTime()));
        store.setOutOfStockAlertTime(LocalTime.parse(request.outOfStockAlertTime()));
        store.setBatchPromotionTime(LocalTime.parse(request.batchPromotionTime()));
        store.setUpdatedAt(LocalDateTime.now());

        Store saved = storeRepository.save(store);
        activityLogService.log(
            principal.getId(),
            store.getId(),
            "ADMIN_STORE_SCHEDULE_UPDATED",
            "STORE",
            store.getId(),
            Map.of(
                "expiryAlertTime", saved.getExpiryAlertTime().toString(),
                "lowStockAlertTime", saved.getLowStockAlertTime().toString(),
                "outOfStockAlertTime", saved.getOutOfStockAlertTime().toString(),
                "batchPromotionTime", saved.getBatchPromotionTime().toString()
            )
        );
        return ResponseEntity.ok(StoreAlertScheduleResponse.from(saved));
    }

    private UserPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }
}
