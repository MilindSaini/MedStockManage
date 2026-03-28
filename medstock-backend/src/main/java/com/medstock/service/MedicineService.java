package com.medstock.service;

import com.medstock.dto.medicine.MedicineResponse;
import com.medstock.dto.medicine.MedicineUpsertRequest;
import com.medstock.entity.Medicine;
import com.medstock.repository.MedicineRepository;
import com.medstock.security.UserPrincipal;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private static final Set<String> ALLOWED_SORTS = Set.of(
        "name",
        "expiryDate",
        "currentStock",
        "lowStockThreshold",
        "updatedAt",
        "createdAt",
        "mrp",
        "purchasePrice"
    );

    private final MedicineRepository medicineRepository;
    private final PermissionGuard permissionGuard;

    @Transactional
    public MedicineResponse addMedicine(UserPrincipal principal, MedicineUpsertRequest request) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertCanAdd(principal, storeId);

        LocalDateTime now = LocalDateTime.now();
        Medicine medicine = new Medicine();
        medicine.setStoreId(storeId);
        applyPayload(medicine, request);
        medicine.setIsDeleted(false);
        medicine.setCreatedBy(principal.getId());
        medicine.setUpdatedBy(principal.getId());
        medicine.setCreatedAt(now);
        medicine.setUpdatedAt(now);

        Medicine saved = medicineRepository.save(medicine);
        return MedicineResponse.from(saved);
    }

    @Transactional
    public MedicineResponse updateMedicine(Long medicineId, UserPrincipal principal, MedicineUpsertRequest request) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertCanEdit(principal, storeId);

        Medicine medicine = getStoreMedicineOrThrow(medicineId, storeId);
        applyPayload(medicine, request);
        medicine.setUpdatedBy(principal.getId());
        medicine.setUpdatedAt(LocalDateTime.now());

        return MedicineResponse.from(medicineRepository.save(medicine));
    }

    @Transactional
    public void softDelete(Long medicineId, UserPrincipal principal) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertCanDelete(principal, storeId);

        Medicine medicine = getStoreMedicineOrThrow(medicineId, storeId);
        medicine.setIsDeleted(true);
        medicine.setUpdatedBy(principal.getId());
        medicine.setUpdatedAt(LocalDateTime.now());
        medicineRepository.save(medicine);
    }

    public MedicineResponse getMedicineById(Long medicineId, UserPrincipal principal) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertStoreAccess(principal, storeId);
        return MedicineResponse.from(getStoreMedicineOrThrow(medicineId, storeId));
    }

    public Page<MedicineResponse> getMedicines(
        UserPrincipal principal,
        String search,
        String category,
        LocalDate expiringBefore,
        Boolean outOfStock,
        int page,
        int size,
        String sortBy,
        String sortDir
    ) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertStoreAccess(principal, storeId);

        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), 100),
            resolveSort(sortBy, sortDir)
        );

        Specification<Medicine> specification = (root, query, cb) -> cb.equal(root.get("storeId"), storeId);
        specification = specification.and((root, query, cb) -> cb.isFalse(root.get("isDeleted")));

        if (search != null && !search.isBlank()) {
            String normalized = "%" + search.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), normalized),
                cb.like(cb.lower(root.get("genericName")), normalized),
                cb.like(cb.lower(root.get("manufacturer")), normalized),
                cb.like(cb.lower(root.get("skuCode")), normalized)
            ));
        }

        if (category != null && !category.isBlank()) {
            specification = specification.and(
                (root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase())
            );
        }

        if (expiringBefore != null) {
            specification = specification.and(
                (root, query, cb) -> cb.lessThanOrEqualTo(root.get("expiryDate"), expiringBefore)
            );
        }

        if (Boolean.TRUE.equals(outOfStock)) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("currentStock"), 0));
        }

        return medicineRepository.findAll(specification, pageable).map(MedicineResponse::from);
    }

    public List<MedicineResponse> getExpiringBefore(UserPrincipal principal, LocalDate date) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertStoreAccess(principal, storeId);
        LocalDate effectiveDate = date != null ? date : LocalDate.now().plusDays(30);
        return medicineRepository.findByStoreIdAndIsDeletedFalseAndExpiryDateLessThanEqual(storeId, effectiveDate)
            .stream()
            .map(MedicineResponse::from)
            .toList();
    }

    public List<MedicineResponse> getOutOfStock(UserPrincipal principal) {
        Long storeId = requireStoreId(principal);
        permissionGuard.assertStoreAccess(principal, storeId);
        return medicineRepository.findByStoreIdAndCurrentStockEqualsAndIsDeletedFalse(storeId, 0)
            .stream()
            .map(MedicineResponse::from)
            .toList();
    }

    private Medicine getStoreMedicineOrThrow(Long medicineId, Long storeId) {
        return medicineRepository.findByIdAndStoreIdAndIsDeletedFalse(medicineId, storeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found"));
    }

    private Long requireStoreId(UserPrincipal principal) {
        if (principal.getStoreId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not attached to any store");
        }
        return principal.getStoreId();
    }

    private Sort resolveSort(String sortBy, String sortDir) {
        String normalizedSortBy = (sortBy == null || sortBy.isBlank()) ? "updatedAt" : sortBy.trim();
        if (!ALLOWED_SORTS.contains(normalizedSortBy)) {
            normalizedSortBy = "updatedAt";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, normalizedSortBy);
    }

    private void applyPayload(Medicine medicine, MedicineUpsertRequest request) {
        medicine.setName(request.name().trim());
        medicine.setGenericName(normalizeNullable(request.genericName()));
        medicine.setCategory(normalizeNullable(request.category()));
        medicine.setManufacturer(normalizeNullable(request.manufacturer()));
        medicine.setSkuCode(normalizeNullable(request.skuCode()));
        medicine.setUnit(normalizeNullable(request.unit()) != null ? request.unit().trim() : "pcs");
        medicine.setMrp(request.mrp());
        medicine.setPurchasePrice(request.purchasePrice());
        medicine.setQuantityAvailable(request.quantityAvailable());
        medicine.setQuantitySold(request.quantitySold());
        medicine.setLowStockThreshold(request.lowStockThreshold());
        medicine.setExpiryDate(request.expiryDate());
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
