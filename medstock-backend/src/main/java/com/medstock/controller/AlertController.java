package com.medstock.controller;

import com.medstock.dto.alert.GroupedAlertsResponse;
import com.medstock.security.UserPrincipal;
import com.medstock.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/grouped")
    public ResponseEntity<GroupedAlertsResponse> grouped(Authentication authentication) {
        UserPrincipal principal = requirePrincipal(authentication);
        if (principal.getStoreId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not attached to any store");
        }
        return ResponseEntity.ok(alertService.getGroupedAlerts(principal.getStoreId()));
    }

    private UserPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }
}
