package com.medstock.service;

import com.medstock.dto.alert.AlertItemResponse;
import com.medstock.dto.alert.GroupedAlertsResponse;
import com.medstock.entity.AlertLog;
import com.medstock.entity.Medicine;
import com.medstock.entity.Store;
import com.medstock.entity.User;
import com.medstock.repository.AlertLogRepository;
import com.medstock.repository.MedicineRepository;
import com.medstock.repository.StoreRepository;
import com.medstock.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private static final String BATCH_PROMOTION_ALERT_TYPE = "BATCH_PROMOTION";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final List<AlertType> GROUP_ORDER = List.of(
        AlertType.CRITICAL,
        AlertType.EXPIRED,
        AlertType.OUT_OF_STOCK,
        AlertType.WARNING,
        AlertType.LOW_STOCK
    );

    private final MedicineRepository medicineRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final AlertLogRepository alertLogRepository;
    private final NotificationService notificationService;

    @Value("${medstock.notifications.alert-dedup-hours:24}")
    private long dedupHours;

    public GroupedAlertsResponse getGroupedAlerts(Long storeId) {
        List<Medicine> medicines = medicineRepository.findByStoreIdAndIsDeletedFalse(storeId);

        Map<String, List<AlertItemResponse>> grouped = new LinkedHashMap<>();
        Map<AlertType, List<Medicine>> groupedDomain = groupByStatus(medicines);

        for (AlertType status : GROUP_ORDER) {
            List<AlertItemResponse> rows = groupedDomain.getOrDefault(status, List.of())
                .stream()
                .map(medicine -> AlertItemResponse.from(medicine, status))
                .toList();
            grouped.put(status.name(), rows);
        }

        return new GroupedAlertsResponse(grouped, LocalDateTime.now());
    }

    public void triggerImmediateStockAlert(Long storeId, Medicine medicine) {
        AlertType status = computeStatus(medicine);
        if (status == AlertType.LOW_STOCK || status == AlertType.OUT_OF_STOCK) {
            sendAlertIfNeeded(storeId, medicine, status, false);
        }
    }

    public void notifyForStoreStatuses(Long storeId, Set<AlertType> targetStatuses) {
        List<Medicine> medicines = medicineRepository.findByStoreIdAndIsDeletedFalse(storeId);

        Map<AlertType, List<Medicine>> pendingByStatus = new EnumMap<>(AlertType.class);
        for (Medicine medicine : medicines) {
            AlertType status = computeStatus(medicine);
            if (!targetStatuses.contains(status)) {
                continue;
            }

            pendingByStatus.computeIfAbsent(status, ignored -> new java.util.ArrayList<>()).add(medicine);
        }

        if (pendingByStatus.isEmpty()) {
            return;
        }

        Store store = storeRepository.findById(storeId).orElse(null);
        String storeName = store != null ? store.getName() : "Your Store";
        List<User> recipients = userRepository.findByStoreIdAndIsActiveTrue(storeId);

        for (Map.Entry<AlertType, List<Medicine>> entry : pendingByStatus.entrySet()) {
            AlertType status = entry.getKey();
            List<Medicine> pendingMedicines = entry.getValue();

            sendScheduledStatusEmail(recipients, storeName, status, pendingMedicines);
            for (Medicine medicine : pendingMedicines) {
                saveAlertLog(
                    storeId,
                    medicine.getId(),
                    status.name(),
                    buildAlertMessage(medicine, status, true)
                );
            }
        }
    }

    public void notifyBatchPromotions(Long storeId, List<Medicine> promotedMedicines) {
        if (promotedMedicines == null || promotedMedicines.isEmpty()) {
            return;
        }

        LocalDateTime since = LocalDateTime.now().minusHours(Math.max(dedupHours, 1));
        List<Medicine> pending = promotedMedicines.stream()
            .filter(medicine -> !wasAlertSentRecently(medicine.getId(), BATCH_PROMOTION_ALERT_TYPE, since))
            .toList();

        if (pending.isEmpty()) {
            return;
        }

        Store store = storeRepository.findById(storeId).orElse(null);
        String storeName = store != null ? store.getName() : "Your Store";
        List<User> recipients = userRepository.findByStoreIdAndIsActiveTrue(storeId);

        sendScheduledBatchPromotionEmail(recipients, storeName, pending);

        for (Medicine medicine : pending) {
            String message = "[daily check] " + medicine.getName()
                + " moved to next available batch after reaching zero stock.";
            saveAlertLog(storeId, medicine.getId(), BATCH_PROMOTION_ALERT_TYPE, message);
        }
    }

    private Map<AlertType, List<Medicine>> groupByStatus(List<Medicine> medicines) {
        Map<AlertType, List<Medicine>> grouped = new EnumMap<>(AlertType.class);
        for (AlertType type : GROUP_ORDER) {
            grouped.put(type, new java.util.ArrayList<>());
        }

        for (Medicine medicine : medicines) {
            AlertType status = computeStatus(medicine);
            if (status == AlertType.OK) {
                continue;
            }
            grouped.computeIfAbsent(status, ignored -> new java.util.ArrayList<>()).add(medicine);
        }

        return grouped;
    }

    private AlertType computeStatus(Medicine medicine) {
        return MedicineStatusUtil.computeStatus(
            medicine.getExpiryDate(),
            medicine.getCurrentStock(),
            medicine.getLowStockThreshold()
        );
    }

    private void sendAlertIfNeeded(Long storeId, Medicine medicine, AlertType status, boolean schedulerTriggered) {
        LocalDateTime since = LocalDateTime.now().minusHours(Math.max(dedupHours, 1));
        if (wasAlertSentRecently(medicine.getId(), status.name(), since)) {
            return;
        }

        Store store = storeRepository.findById(storeId).orElse(null);
        String storeName = store != null ? store.getName() : "Your Store";

        String message = buildAlertMessage(medicine, status, schedulerTriggered);

        List<User> recipients = userRepository.findByStoreIdAndIsActiveTrue(storeId);
        for (User user : recipients) {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                try {
                    String template = (status == AlertType.EXPIRED || status == AlertType.CRITICAL || status == AlertType.WARNING)
                        ? "expiry-alert"
                        : "low-stock-alert";
                    String html = notificationService.renderTemplate(template, Map.of(
                        "fullName", defaultName(user.getFullName()),
                        "storeName", storeName,
                        "medicineName", medicine.getName(),
                        "status", status.name(),
                        "currentStock", String.valueOf(medicine.getCurrentStock()),
                        "threshold", String.valueOf(medicine.getLowStockThreshold()),
                        "expiryDate", String.valueOf(medicine.getExpiryDate()),
                        "message", message
                    ));
                    notificationService.sendEmail(user.getEmail(), "MedStock Alert: " + status.name(), html);
                } catch (Exception ex) {
                    log.warn("Failed to send email alert for medicine {} to user {}", medicine.getId(), user.getId(), ex);
                }
            }
        }

        saveAlertLog(storeId, medicine.getId(), status.name(), message);
    }

    private void sendScheduledStatusEmail(
        List<User> recipients,
        String storeName,
        AlertType status,
        List<Medicine> medicines
    ) {
        if (recipients == null || recipients.isEmpty() || medicines == null || medicines.isEmpty()) {
            return;
        }

        String subject = "MedStock Alert: " + status.name();
        String intro = switch (status) {
            case EXPIRED -> "The following medicines are expired and must be isolated immediately.";
            case CRITICAL -> "The following medicines expire within 7 days. Prioritize sale or usage.";
            case WARNING -> "The following medicines expire within 30 days. Plan replacement stock.";
            case LOW_STOCK -> "The following medicines are at or below configured stock threshold.";
            case OUT_OF_STOCK -> "The following medicines are out of stock. Restock urgently.";
            case OK -> "";
        };

        List<ScheduledAlertItem> items = medicines.stream()
            .map(medicine -> new ScheduledAlertItem(
                medicine.getName(),
                status.name(),
                medicine.getCurrentStock(),
                medicine.getLowStockThreshold(),
                formatDate(medicine.getExpiryDate()),
                buildAlertMessage(medicine, status, true)
            ))
            .toList();

        for (User user : recipients) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }

            try {
                String html = notificationService.renderTemplate("scheduled-alert-summary", Map.of(
                    "fullName", defaultName(user.getFullName()),
                    "storeName", storeName,
                    "subjectLabel", status.name(),
                    "intro", intro,
                    "items", items
                ));
                notificationService.sendEmail(user.getEmail(), subject, html);
            } catch (Exception ex) {
                log.warn("Failed to send scheduled {} alert summary to user {}", status.name(), user.getId(), ex);
            }
        }
    }

    private void sendScheduledBatchPromotionEmail(
        List<User> recipients,
        String storeName,
        List<Medicine> medicines
    ) {
        if (recipients == null || recipients.isEmpty() || medicines == null || medicines.isEmpty()) {
            return;
        }

        List<ScheduledAlertItem> items = medicines.stream()
            .map(medicine -> new ScheduledAlertItem(
                medicine.getName(),
                BATCH_PROMOTION_ALERT_TYPE,
                medicine.getCurrentStock(),
                medicine.getLowStockThreshold(),
                formatDate(medicine.getExpiryDate()),
                "[daily check] " + medicine.getName() + " moved to next available batch after reaching zero stock."
            ))
            .toList();

        for (User user : recipients) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }

            try {
                String html = notificationService.renderTemplate("scheduled-alert-summary", Map.of(
                    "fullName", defaultName(user.getFullName()),
                    "storeName", storeName,
                    "subjectLabel", BATCH_PROMOTION_ALERT_TYPE,
                    "intro", "The following medicines were automatically promoted to their next available batch.",
                    "items", items
                ));
                notificationService.sendEmail(user.getEmail(), "MedStock Alert: " + BATCH_PROMOTION_ALERT_TYPE, html);
            } catch (Exception ex) {
                log.warn("Failed to send batch promotion summary to user {}", user.getId(), ex);
            }
        }
    }

    private boolean wasAlertSentRecently(Long medicineId, String alertType, LocalDateTime since) {
        return alertLogRepository.existsByMedicineIdAndAlertTypeAndCreatedAtAfter(medicineId, alertType, since);
    }

    private void saveAlertLog(Long storeId, Long medicineId, String alertType, String message) {
        AlertLog logEntry = new AlertLog();
        logEntry.setStoreId(storeId);
        logEntry.setMedicineId(medicineId);
        logEntry.setAlertType(alertType);
        logEntry.setMessage(message);
        logEntry.setIsRead(false);
        logEntry.setCreatedAt(LocalDateTime.now());
        alertLogRepository.save(logEntry);
    }

    private String formatDate(java.time.LocalDate date) {
        if (date == null) {
            return "-";
        }
        return DATE_FORMATTER.format(date);
    }

    private String buildAlertMessage(Medicine medicine, AlertType status, boolean schedulerTriggered) {
        String source = schedulerTriggered ? "daily check" : "real-time stock update";

        return switch (status) {
            case EXPIRED -> "[" + source + "] " + medicine.getName() + " is expired. Stop dispensing and isolate stock.";
            case CRITICAL -> "[" + source + "] " + medicine.getName() + " expires within 7 days. Prioritize sale/usage.";
            case WARNING -> "[" + source + "] " + medicine.getName() + " expires within 30 days. Plan replacement stock.";
            case LOW_STOCK -> "[" + source + "] " + medicine.getName() + " is at low stock ("
                + medicine.getCurrentStock() + " <= " + medicine.getLowStockThreshold() + ").";
            case OUT_OF_STOCK -> "[" + source + "] " + medicine.getName() + " is out of stock. Restock urgently.";
            case OK -> "";
        };
    }

    private String defaultName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Team";
        }
        return fullName.trim();
    }

    public static record ScheduledAlertItem(
        String medicineName,
        String status,
        Integer currentStock,
        Integer threshold,
        String expiryDate,
        String message
    ) {
    }
}
