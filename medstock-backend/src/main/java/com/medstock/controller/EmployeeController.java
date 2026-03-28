package com.medstock.controller;

import com.medstock.dto.employee.AddEmployeeRequest;
import com.medstock.dto.employee.EmployeePermissionsPayload;
import com.medstock.dto.employee.EmployeeResponse;
import com.medstock.dto.employee.UpdateEmployeePermissionsRequest;
import com.medstock.security.UserPrincipal;
import com.medstock.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> listEmployees(Authentication authentication) {
        return ResponseEntity.ok(employeeService.listEmployees(requirePrincipal(authentication)));
    }

    @GetMapping("/my-permissions")
    public ResponseEntity<EmployeePermissionsPayload> getMyPermissions(Authentication authentication) {
        return ResponseEntity.ok(employeeService.getMyPermissions(requirePrincipal(authentication)));
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> addEmployee(
        @Valid @RequestBody AddEmployeeRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(employeeService.addEmployee(requirePrincipal(authentication), request));
    }

    @PutMapping("/{employeeUserId}/permissions")
    public ResponseEntity<EmployeeResponse> updatePermissions(
        @PathVariable Long employeeUserId,
        @Valid @RequestBody UpdateEmployeePermissionsRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(
            employeeService.updatePermissions(
                requirePrincipal(authentication),
                employeeUserId,
                request.permissions()
            )
        );
    }

    @DeleteMapping("/{employeeUserId}")
    public ResponseEntity<Void> removeEmployee(@PathVariable Long employeeUserId, Authentication authentication) {
        employeeService.removeEmployee(requirePrincipal(authentication), employeeUserId);
        return ResponseEntity.noContent().build();
    }

    private UserPrincipal requirePrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }
}
