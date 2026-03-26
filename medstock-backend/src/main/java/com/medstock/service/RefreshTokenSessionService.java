package com.medstock.service;

import com.medstock.entity.RefreshTokenSession;
import com.medstock.repository.RefreshTokenSessionRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenSessionService {

    private final RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Transactional
    public void storeRefreshToken(String tokenId, Long userId, String rawToken, Instant expiresAt) {
        refreshTokenSessionRepository.deleteByExpiresAtBefore(Instant.now());

        RefreshTokenSession session = new RefreshTokenSession();
        session.setId(tokenId);
        session.setUserId(userId);
        session.setTokenHash(hash(rawToken));
        session.setExpiresAt(expiresAt);
        session.setCreatedAt(Instant.now());
        refreshTokenSessionRepository.save(session);
    }

    @Transactional
    public boolean consumeRefreshToken(String tokenId, Long userId, String rawToken) {
        int deleted = refreshTokenSessionRepository.consumeToken(
            tokenId,
            userId,
            hash(rawToken),
            Instant.now()
        );
        return deleted == 1;
    }

    @Transactional
    public void revokeRefreshToken(String tokenId, Long userId) {
        refreshTokenSessionRepository.revokeToken(tokenId, userId, Instant.now());
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash refresh token", exception);
        }
    }
}
