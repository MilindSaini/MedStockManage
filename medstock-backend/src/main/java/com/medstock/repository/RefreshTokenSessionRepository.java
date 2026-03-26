package com.medstock.repository;

import com.medstock.entity.RefreshTokenSession;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM RefreshTokenSession r
        WHERE r.expiresAt < :timestamp
    """)
    int deleteByExpiresAtBefore(@Param("timestamp") Instant timestamp);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM RefreshTokenSession r
        WHERE r.id = :id
          AND r.userId = :userId
          AND r.tokenHash = :tokenHash
          AND r.revokedAt IS NULL
          AND r.expiresAt > :now
    """)
    int consumeToken(
        @Param("id") String id,
        @Param("userId") Long userId,
        @Param("tokenHash") String tokenHash,
        @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE RefreshTokenSession r
        SET r.revokedAt = :now
        WHERE r.id = :id
          AND r.userId = :userId
          AND r.revokedAt IS NULL
    """)
    int revokeToken(@Param("id") String id, @Param("userId") Long userId, @Param("now") Instant now);
}
