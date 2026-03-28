package com.medstock.repository;

import com.medstock.entity.BatchGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchGroupRepository extends JpaRepository<BatchGroup, Long> {

    Optional<BatchGroup> findFirstByStoreIdAndMedicineIdAndIsActiveFalseAndQuantityGreaterThanOrderByExpiryDateAscCreatedAtAsc(
        Long storeId,
        Long medicineId,
        Integer quantity
    );

    Optional<BatchGroup> findFirstByStoreIdAndMedicineIdAndIsActiveTrueOrderByUpdatedAtDesc(Long storeId, Long medicineId);
}
