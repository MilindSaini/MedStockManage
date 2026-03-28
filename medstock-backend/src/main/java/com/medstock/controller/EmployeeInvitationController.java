package com.medstock.controller;

import com.medstock.dto.employee.EmployeeInvitationResponse;
import com.medstock.dto.employee.RespondEmployeeInvitationRequest;
import com.medstock.security.UserPrincipal;
import com.medstock.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/employee-invitations")
@RequiredArgsConstructor
public class EmployeeInvitationController {

    private final EmployeeService employeeService;

    @GetMapping("/me")
    public ResponseEntity<List<EmployeeInvitationResponse>> listMyInvitations(Authentication authentication) {
        return ResponseEntity.ok(employeeService.listPendingInvitations(requirePrincipal(authentication)));
    }

    @PostMapping("/{invitationId}/respond")
    public ResponseEntity<Void> respondToInvitation(
        @PathVariable Long invitationId,
        @Valid @RequestBody RespondEmployeeInvitationRequest request,
        Authentication authentication
    ) {
        employeeService.respondToInvitation(requirePrincipal(authentication), invitationId, Boolean.TRUE.equals(request.accept()));
        return ResponseEntity.noContent().build();
    }

    private UserPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }
}
