package com.example.visceralmassageapi.auth.repo;

import com.example.visceralmassageapi.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken token
            SET token.revokedAt = :revokedAt
            WHERE token.user.id = :userId
              AND token.revokedAt IS NULL
            """)
    void revokeActiveTokensForUser(long userId, OffsetDateTime revokedAt);
}
