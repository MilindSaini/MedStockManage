package com.medstock.controller;

import com.medstock.dto.admin.AiProviderAdminResponse;
import com.medstock.dto.admin.AiProviderTestResponse;
import com.medstock.dto.admin.UpdateAiProviderRequest;
import com.medstock.security.UserPrincipal;
import com.medstock.service.AiProviderAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/ai-providers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiProviderAdminController {

    private final AiProviderAdminService aiProviderAdminService;

    @GetMapping
    public ResponseEntity<List<AiProviderAdminResponse>> list() {
        return ResponseEntity.ok(aiProviderAdminService.list());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AiProviderAdminResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateAiProviderRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(aiProviderAdminService.update(id, request, requirePrincipal(authentication)));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<AiProviderTestResponse> test(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(aiProviderAdminService.test(id, requirePrincipal(authentication)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<AiProviderAdminResponse> activate(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(aiProviderAdminService.activate(id, requirePrincipal(authentication)));
    }

    private UserPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }
}
