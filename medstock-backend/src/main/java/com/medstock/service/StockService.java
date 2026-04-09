package com.medstock.service;

import com.medstock.dto.stock.StockAdjustResponse;
import com.medstock.entity.Medicine;
import com.medstock.entity.StockTransaction;
import com.medstock.repository.MedicineRepository;
import com.medstock.repository.StockTransactionRepository;
import com.medstock.security.UserPrincipal;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class StockService {

    private final MedicineRepository medicineRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final PermissionGuard permissionGuard;
    private final BatchService batchService;
    private final ActivityLogService activityLogService;

    @Transactional
    public StockAdjustResponse adjustStock(UserPrincipal principal, Long medicineId, Integer delta, String type, String notes) {
        Long storeId = requireStoreId(principal);

        if (delta == null || delta == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "delta must be non-zero");
        }

        if (delta > 0) {
            permissionGuard.assertCanAdd(principal, storeId);
        } else {
            permissionGuard.assertCanSell(principal, storeId);
        }

        Medicine medicine = medicineRepository.findByIdAndStoreIdAndIsDeletedFalse(medicineId, storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found"));

        int currentStock = currentStock(medicine);
        if (delta < 0 && currentStock + delta < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock");
        }

        if (delta > 0) {
            medicine.setQuantityAvailable(medicine.getQuantityAvailable() + delta);
        } else {
            medicine.setQuantitySold(medicine.getQuantitySold() + Math.abs(delta));
        }

        LocalDateTime now = LocalDateTime.now();
        medicine.setUpdatedBy(principal.getId());
        medicine.setUpdatedAt(now);
        Medicine savedMedicine = medicineRepository.save(medicine);

        StockTransaction transaction = new StockTransaction();
        transaction.setStoreId(storeId);
        transaction.setMedicineId(savedMedicine.getId());
        transaction.setUserId(principal.getId());
        transaction.setTransactionType(type == null || type.isBlank() ? (delta > 0 ? "RESTOCK" : "SALE") : type.trim());
        transaction.setDelta(delta);
        transaction.setNotes(normalizeNullable(notes));
        transaction.setCreatedAt(now);
        stockTransactionRepository.save(transaction);

        int nextCurrentStock = currentStock(savedMedicine);
        if (nextCurrentStock == 0) {
            batchService.onStockZero(savedMedicine.getId(), storeId);
        }

        activityLogService.log(
            principal.getId(),
            storeId,
            "STOCK_ADJUSTED",
            "MEDICINE",
            savedMedicine.getId(),
            Map.of(
                "delta", delta,
                "transactionType", transaction.getTransactionType(),
                "nextCurrentStock", nextCurrentStock
            )
        );

        return new StockAdjustResponse(
            savedMedicine.getId(),
            delta,
            nextCurrentStock,
            savedMedicine.getQuantityAvailable(),
            savedMedicine.getQuantitySold(),
            transaction.getTransactionType(),
            "Stock adjusted successfully"
        );
    }

    private Long requireStoreId(UserPrincipal principal) {
        if (principal.getStoreId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not attached to any store");
        }
        return principal.getStoreId();
    }

    private int currentStock(Medicine medicine) {
        if (medicine.getCurrentStock() != null) {
            return medicine.getCurrentStock();
        }
        return Math.max((medicine.getQuantityAvailable() - medicine.getQuantitySold()), 0);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
