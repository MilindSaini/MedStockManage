package com.medstock.service;

import com.medstock.dto.auth.AuthResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class OAuthLoginCodeService {

    private static final long CODE_TTL_SECONDS = 60;

    private final ConcurrentHashMap<String, OAuthCodeEntry> codeStore = new ConcurrentHashMap<>();

    public String createCode(AuthResponse authResponse) {
        cleanupExpiredCodes();
        String code = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(CODE_TTL_SECONDS);
        codeStore.put(code, new OAuthCodeEntry(authResponse, expiresAt));
        return code;
    }

    public Optional<AuthResponse> consumeCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        OAuthCodeEntry entry = codeStore.remove(code);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return Optional.empty();
        }

        return Optional.of(entry.authResponse());
    }

    private void cleanupExpiredCodes() {
        Instant now = Instant.now();
        codeStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private record OAuthCodeEntry(AuthResponse authResponse, Instant expiresAt) {
    }
}
