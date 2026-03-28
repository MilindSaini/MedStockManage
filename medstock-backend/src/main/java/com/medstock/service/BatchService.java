package com.medstock.service;

import com.medstock.entity.BatchGroup;
import com.medstock.entity.Medicine;
import com.medstock.repository.BatchGroupRepository;
import com.medstock.repository.MedicineRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchGroupRepository batchGroupRepository;
    private final MedicineRepository medicineRepository;

    @Transactional
    public boolean onStockZero(Long medicineId, Long storeId) {
        Medicine medicine = medicineRepository.findByIdAndStoreIdAndIsDeletedFalse(medicineId, storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found"));

        BatchGroup nextBatch = batchGroupRepository
            .findFirstByStoreIdAndMedicineIdAndIsActiveFalseAndQuantityGreaterThanOrderByExpiryDateAscCreatedAtAsc(
                storeId,
                medicineId,
                0
            )
            .orElse(null);

        if (nextBatch == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        batchGroupRepository.findFirstByStoreIdAndMedicineIdAndIsActiveTrueOrderByUpdatedAtDesc(storeId, medicineId)
            .ifPresent(activeBatch -> {
                activeBatch.setIsActive(false);
                activeBatch.setUpdatedAt(now);
                batchGroupRepository.save(activeBatch);
            });

        nextBatch.setIsActive(true);
        nextBatch.setUpdatedAt(now);
        batchGroupRepository.save(nextBatch);

        medicine.setActiveBatchId(nextBatch.getId());
        medicine.setQuantityAvailable(medicine.getQuantitySold() + Math.max(nextBatch.getQuantity(), 0));
        medicine.setUpdatedAt(now);
        medicineRepository.save(medicine);

        return true;
    }
}
