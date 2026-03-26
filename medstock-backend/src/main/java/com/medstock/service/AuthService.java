package com.medstock.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.medstock.dto.auth.AuthLoginRequest;
import com.medstock.dto.auth.AuthRegisterRequest;
import com.medstock.dto.auth.AuthResponse;
import com.medstock.dto.auth.AuthUserResponse;
import com.medstock.dto.auth.OwnerProfileRequest;
import com.medstock.entity.Store;
import com.medstock.entity.User;
import com.medstock.repository.StoreRepository;
import com.medstock.repository.UserRepository;
import com.medstock.security.RoleUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final com.medstock.security.JwtUtil jwtUtil;
    private final RefreshTokenSessionService refreshTokenSessionService;
    private final OAuthLoginCodeService oAuthLoginCodeService;

    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedUsername = normalizeUsername(request.username());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
        }

        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setStoreId(null);
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setFullName(null);
        user.setPhone(null);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("EMPLOYEE");
        user.setIsActive(true);
        user.setEmailVerified(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        return issueTokens(user);
    }

    public AuthResponse login(AuthLoginRequest request) {
        String normalizedIdentity = normalizeIdentity(request.identifier());
        try {
            authenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(normalizedIdentity, request.password())
            );
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = userRepository.findByUsernameOrEmail(normalizedIdentity, normalizedIdentity)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        ensureUserActive(user);

        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtUtil.validateToken(refreshToken, "REFRESH")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        DecodedJWT decodedJWT = jwtUtil.extractClaims(refreshToken);
        Long userId = decodedJWT.getClaim("uid").asLong();
        String tokenId = decodedJWT.getId();

        if (userId == null || tokenId == null || tokenId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        ensureUserActive(user);

        boolean consumed = refreshTokenSessionService.consumeRefreshToken(tokenId, userId, refreshToken);
        if (!consumed) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtUtil.validateToken(refreshToken, "REFRESH")) {
            return;
        }

        DecodedJWT decodedJWT = jwtUtil.extractClaims(refreshToken);
        Long userId = decodedJWT.getClaim("uid").asLong();
        String tokenId = decodedJWT.getId();
        if (userId == null || tokenId == null || tokenId.isBlank()) {
            return;
        }

        refreshTokenSessionService.revokeRefreshToken(tokenId, userId);
    }

    public AuthUserResponse me(String identity) {
        String normalizedIdentity = normalizeIdentity(identity);
        User user = userRepository.findByUsernameOrEmail(normalizedIdentity, normalizedIdentity)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ensureUserActive(user);
        return AuthUserResponse.from(user);
    }

    @Transactional
    public AuthResponse loginOrCreateGoogleUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseGet(() -> createGoogleUser(normalizedEmail));

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername(generateUniqueGoogleUsername(normalizedEmail));
        }

        ensureUserActive(user);

        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return issueTokens(user);
    }

    public AuthResponse exchangeOAuthCode(String code) {
        return oAuthLoginCodeService.consumeCode(code)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OAuth exchange code"));
    }

    private User createGoogleUser(String normalizedEmail) {
        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setStoreId(null);
        user.setUsername(generateUniqueGoogleUsername(normalizedEmail));
        user.setEmail(normalizedEmail);
        user.setFullName(null);
        user.setPhone(null);
        user.setPasswordHash(null);
        user.setRole("EMPLOYEE");
        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    @Transactional
    public AuthUserResponse completeOwnerProfile(String identity, OwnerProfileRequest request) {
        String normalizedIdentity = normalizeIdentity(identity);
        User user = userRepository.findByUsernameOrEmail(normalizedIdentity, normalizedIdentity)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ensureUserActive(user);

        if (RoleUtils.hasRole(user.getRole(), "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin profile cannot be changed to owner");
        }

        if (RoleUtils.hasRole(user.getRole(), "OWNER")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already an owner");
        }

        String normalizedPhone = request.phone().trim();
        if (userRepository.existsByPhoneAndIdNot(normalizedPhone, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already in use");
        }

        LocalDateTime now = LocalDateTime.now();

        Store store = new Store();
        store.setName(request.storeName().trim());
        store.setSubscriptionStatus("TRIAL");
        store.setTrialEndsAt(now.plusDays(30));
        store.setCreatedAt(now);
        store.setUpdatedAt(now);
        store = storeRepository.save(store);

        user.setStoreId(store.getId());
        user.setFullName(request.fullName().trim());
        user.setPhone(normalizedPhone);
        user.setRole(RoleUtils.serializeRoles(RoleUtils.addRole(user.getRole(), "OWNER")));
        user.setUpdatedAt(now);
        user = userRepository.save(user);

        store.setOwnerUserId(user.getId());
        store.setUpdatedAt(now);
        storeRepository.save(store);

        return AuthUserResponse.from(user);
    }

    private AuthResponse issueTokens(User user) {
        ensureUserActive(user);

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenId = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(user, refreshTokenId);
        DecodedJWT refreshClaims = jwtUtil.extractClaims(refreshToken);
        refreshTokenSessionService.storeRefreshToken(
            refreshTokenId,
            user.getId(),
            refreshToken,
            refreshClaims.getExpiresAt().toInstant()
        );

        return new AuthResponse(
            accessToken,
            refreshToken,
            "Bearer",
            jwtUtil.getAccessTokenExpiresInSeconds(),
            AuthUserResponse.from(user)
        );
    }

    private org.springframework.security.authentication.AuthenticationManager authenticationManager() {
        try {
            return authenticationConfiguration.getAuthenticationManager();
        } catch (Exception exception) {
            throw new IllegalStateException("Authentication manager is unavailable", exception);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeIdentity(String identity) {
        String trimmed = normalizeNullable(identity);
        if (trimmed == null) {
            return "";
        }
        if (trimmed.contains("@")) {
            return trimmed.toLowerCase();
        }
        return trimmed;
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeNullable(email);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        return normalized.toLowerCase();
    }

    private String normalizeUsername(String username) {
        String normalized = normalizeNullable(username);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        return normalized;
    }

    private String generateUniqueGoogleUsername(String email) {
        String base = email == null ? "user" : email.split("@")[0];
        base = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        if (base.isBlank()) {
            base = "user";
        }

        base = base.length() > 50 ? base.substring(0, 50) : base;

        String candidate = base;
        int counter = 1;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            String suffix = String.valueOf(counter++);
            int maxBaseLength = 60 - suffix.length();
            String compactBase = base.length() > maxBaseLength ? base.substring(0, maxBaseLength) : base;
            candidate = compactBase + suffix;
        }

        return candidate;
    }

    private void ensureUserActive(User user) {
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is disabled");
        }
    }
}
