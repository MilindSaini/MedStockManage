package com.medstock.controller;

import com.medstock.dto.admin.AdminActivityRowResponse;
import com.medstock.dto.admin.AdminStoreRowResponse;
import com.medstock.dto.admin.AdminUserRowResponse;
import com.medstock.repository.ActivityLogRepository;
import com.medstock.repository.StoreRepository;
import com.medstock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ActivityLogRepository activityLogRepository;

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserRowResponse>> users(
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminUserRowResponse> rows = userRepository.findForAdmin(search, pageable).map(AdminUserRowResponse::from);
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/stores")
    public ResponseEntity<Page<AdminStoreRowResponse>> stores(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminStoreRowResponse> rows = storeRepository.findAll(pageable).map(AdminStoreRowResponse::from);
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/activity")
    public ResponseEntity<Page<AdminActivityRowResponse>> activity(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<AdminActivityRowResponse> rows = activityLogRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(AdminActivityRowResponse::from);
        return ResponseEntity.ok(rows);
    }

}
