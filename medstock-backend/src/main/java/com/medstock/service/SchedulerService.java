package com.medstock.service;

import com.medstock.entity.Medicine;
import com.medstock.repository.MedicineRepository;
import com.medstock.repository.StoreRepository;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final AlertService alertService;
    private final BatchService batchService;
    private final StoreRepository storeRepository;
    private final MedicineRepository medicineRepository;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void checkBatchPromotion() {
        LocalTime nowMinute = ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).toLocalTime().withSecond(0).withNano(0);
        storeRepository.findAll().forEach(store -> {
            if (!shouldRunAt(nowMinute, store.getBatchPromotionTime(), LocalTime.of(6, 0))) {
                return;
            }
            try {
                java.util.List<Medicine> promotedMedicines = new ArrayList<>();
                for (Medicine medicine : medicineRepository.findByStoreIdAndCurrentStockEqualsAndIsDeletedFalse(store.getId(), 0)) {
                    boolean promoted = batchService.onStockZero(medicine.getId(), store.getId());
                    if (promoted) {
                        promotedMedicines.add(medicine);
                    }
                }
                alertService.notifyBatchPromotions(store.getId(), promotedMedicines);
            } catch (Exception ex) {
                log.warn("Batch promotion scheduler failed for store {}", store.getId(), ex);
            }
        });
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void checkExpiryAlerts() {
        LocalTime nowMinute = ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).toLocalTime().withSecond(0).withNano(0);
        storeRepository.findAll().forEach(store -> {
            if (!shouldRunAt(nowMinute, store.getExpiryAlertTime(), LocalTime.of(8, 0))) {
                return;
            }
            try {
                alertService.notifyForStoreStatuses(store.getId(), Set.of(AlertType.EXPIRED, AlertType.CRITICAL, AlertType.WARNING));
            } catch (Exception ex) {
                log.warn("Expiry scheduler failed for store {}", store.getId(), ex);
            }
        });
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void checkLowStock() {
        LocalTime nowMinute = ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).toLocalTime().withSecond(0).withNano(0);
        storeRepository.findAll().forEach(store -> {
            if (!shouldRunAt(nowMinute, store.getLowStockAlertTime(), LocalTime.of(8, 30))) {
                return;
            }
            try {
                alertService.notifyForStoreStatuses(store.getId(), Set.of(AlertType.LOW_STOCK));
            } catch (Exception ex) {
                log.warn("Low-stock scheduler failed for store {}", store.getId(), ex);
            }
        });
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void checkOutOfStock() {
        LocalTime nowMinute = ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).toLocalTime().withSecond(0).withNano(0);
        storeRepository.findAll().forEach(store -> {
            if (!shouldRunAt(nowMinute, store.getOutOfStockAlertTime(), LocalTime.of(9, 0))) {
                return;
            }
            try {
                alertService.notifyForStoreStatuses(store.getId(), Set.of(AlertType.OUT_OF_STOCK));
            } catch (Exception ex) {
                log.warn("Out-of-stock scheduler failed for store {}", store.getId(), ex);
            }
        });
    }

    private boolean shouldRunAt(LocalTime nowMinute, LocalTime configured, LocalTime fallback) {
        LocalTime target = configured == null ? fallback : configured;
        return nowMinute.getHour() == target.getHour() && nowMinute.getMinute() == target.getMinute();
    }
}
