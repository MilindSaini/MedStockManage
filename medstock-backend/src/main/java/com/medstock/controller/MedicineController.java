package com.medstock.controller;

import com.medstock.dto.medicine.MedicineResponse;
import com.medstock.dto.medicine.MedicineUpsertRequest;
import com.medstock.security.UserPrincipal;
import com.medstock.service.MedicineService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @PostMapping
    public ResponseEntity<MedicineResponse> addMedicine(
        @Valid @RequestBody MedicineUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(medicineService.addMedicine(requirePrincipal(authentication), request));
    }

    @PutMapping("/{medicineId}")
    public ResponseEntity<MedicineResponse> updateMedicine(
        @PathVariable Long medicineId,
        @Valid @RequestBody MedicineUpsertRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(medicineService.updateMedicine(medicineId, requirePrincipal(authentication), request));
    }

    @DeleteMapping("/{medicineId}")
    public ResponseEntity<Void> deleteMedicine(@PathVariable Long medicineId, Authentication authentication) {
        medicineService.softDelete(medicineId, requirePrincipal(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{medicineId}")
    public ResponseEntity<MedicineResponse> getMedicine(@PathVariable Long medicineId, Authentication authentication) {
        return ResponseEntity.ok(medicineService.getMedicineById(medicineId, requirePrincipal(authentication)));
    }

    @GetMapping
    public ResponseEntity<Page<MedicineResponse>> getMedicines(
        Authentication authentication,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) LocalDate expiringBefore,
        @RequestParam(required = false) Boolean outOfStock,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "updatedAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(
            medicineService.getMedicines(
                requirePrincipal(authentication),
                search,
                category,
                expiringBefore,
                outOfStock,
                page,
                size,
                sortBy,
                sortDir
            )
        );
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<MedicineResponse>> getExpiringMedicines(
        Authentication authentication,
        @RequestParam(required = false) LocalDate before
    ) {
        return ResponseEntity.ok(medicineService.getExpiringBefore(requirePrincipal(authentication), before));
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<List<MedicineResponse>> getOutOfStockMedicines(Authentication authentication) {
        return ResponseEntity.ok(medicineService.getOutOfStock(requirePrincipal(authentication)));
    }

    private UserPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }
}
