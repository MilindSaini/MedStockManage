package com.medstock.repository;

import com.medstock.entity.AlertLog;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    boolean existsByMedicineIdAndAlertTypeAndCreatedAtAfter(
        Long medicineId,
        String alertType,
        LocalDateTime since
    );
}
