package com.medstock.repository;

import com.medstock.entity.Medicine;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MedicineRepository extends JpaRepository<Medicine, Long>, JpaSpecificationExecutor<Medicine> {

    List<Medicine> findByStoreIdAndIsDeletedFalse(Long storeId);

    List<Medicine> findByStoreIdAndIsDeletedFalseAndExpiryDateLessThanEqual(Long storeId, LocalDate expiryDate);

    List<Medicine> findByStoreIdAndCurrentStockEqualsAndIsDeletedFalse(Long storeId, Integer currentStock);

    Optional<Medicine> findByIdAndStoreIdAndIsDeletedFalse(Long id, Long storeId);
}
