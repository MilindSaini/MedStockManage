package com.medstock.controller;

import com.medstock.dto.stock.StockAdjustRequest;
import com.medstock.dto.stock.StockAdjustResponse;
import com.medstock.security.UserPrincipal;
import com.medstock.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @PostMapping("/adjust")
    public ResponseEntity<StockAdjustResponse> adjustStock(
        @Valid @RequestBody StockAdjustRequest request,
        Authentication authentication
    ) {
        UserPrincipal principal = requirePrincipal(authentication);
        StockAdjustResponse response = stockService.adjustStock(
            principal,
            request.medicineId(),
            request.delta(),
            request.transactionType(),
            request.notes()
        );
        return ResponseEntity.ok(response);
    }

    private UserPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }
}
