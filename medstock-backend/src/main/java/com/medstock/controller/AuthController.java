package com.medstock.controller;

import com.medstock.dto.auth.AuthLoginRequest;
import com.medstock.dto.auth.AuthRegisterRequest;
import com.medstock.dto.auth.AuthResponse;
import com.medstock.dto.auth.AuthUserResponse;
import com.medstock.dto.auth.OwnerProfileRequest;
import com.medstock.dto.auth.RefreshTokenRequest;
import com.medstock.dto.auth.SendPhoneOtpRequest;
import com.medstock.dto.auth.UpdateProfileRequest;
import com.medstock.dto.auth.VerifyPhoneOtpRequest;
import com.medstock.entity.User;
import com.medstock.security.AuthCookieService;
import com.medstock.security.UserPrincipal;
import com.medstock.service.AuthService;
import com.medstock.service.PhoneVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final PhoneVerificationService phoneVerificationService;

    @Value("${server.port:8080}")
    private String serverPort;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
        @Valid @RequestBody AuthRegisterRequest request,
        HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.register(request);
        authCookieService.setRefreshTokenCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(sanitizeRefreshToken(authResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        authCookieService.setRefreshTokenCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(sanitizeRefreshToken(authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        @Valid @RequestBody(required = false) RefreshTokenRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse response
    ) {
        String refreshToken = authCookieService.extractRefreshToken(servletRequest)
            .orElse(request != null ? request.refreshToken() : null);

        AuthResponse authResponse = authService.refreshToken(refreshToken);
        authCookieService.setRefreshTokenCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(sanitizeRefreshToken(authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(authCookieService.extractRefreshToken(request).orElse(null));
        authCookieService.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/oauth2/exchange")
    public ResponseEntity<AuthResponse> exchangeOauth2Code(
        @RequestParam("code") @NotBlank(message = "OAuth exchange code is required") String code,
        HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.exchangeOAuthCode(code);
        authCookieService.setRefreshTokenCookie(response, authResponse.refreshToken());
        return ResponseEntity.ok(sanitizeRefreshToken(authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return ResponseEntity.ok(authService.me(principal.getUsername()));
    }

    @PostMapping("/owner-profile")
    public ResponseEntity<AuthUserResponse> completeOwnerProfile(
        @Valid @RequestBody OwnerProfileRequest request,
        Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return ResponseEntity.ok(authService.completeOwnerProfile(principal.getUsername(), request));
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthUserResponse> updateProfile(
        @Valid @RequestBody UpdateProfileRequest request,
        Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return ResponseEntity.ok(authService.updateProfile(principal.getUsername(), request));
    }

    @PostMapping("/phone/send-otp")
    public ResponseEntity<Map<String, Object>> sendPhoneOtp(
        @Valid @RequestBody(required = false) SendPhoneOtpRequest request,
        Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        User user = authService.getActiveUserByIdentity(principal.getUsername());
        var result = phoneVerificationService.sendOtp(user, request != null ? request.phone() : null);
        Map<String, Object> payload = new HashMap<>();
        payload.put("accepted", result.accepted());
        payload.put("to", result.to());
        payload.put("reason", result.reason());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/phone/verify-otp")
    public ResponseEntity<AuthUserResponse> verifyPhoneOtp(
        @Valid @RequestBody VerifyPhoneOtpRequest request,
        Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        User user = authService.getActiveUserByIdentity(principal.getUsername());
        phoneVerificationService.verifyOtp(user, request.otp());
        return ResponseEntity.ok(authService.me(principal.getUsername()));
    }

    @GetMapping("/oauth2/google-url")
    public ResponseEntity<Map<String, String>> googleUrl() {
        return ResponseEntity.ok(Map.of("url", "http://localhost:" + serverPort + "/oauth2/authorization/google"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "phase", "2"));
    }

    private AuthResponse sanitizeRefreshToken(AuthResponse authResponse) {
        return new AuthResponse(
            authResponse.accessToken(),
            null,
            authResponse.tokenType(),
            authResponse.expiresIn(),
            authResponse.user()
        );
    }
}
