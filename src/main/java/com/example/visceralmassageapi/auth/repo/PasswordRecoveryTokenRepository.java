package com.example.visceralmassageapi.auth.repo;

import com.example.visceralmassageapi.auth.domain.PasswordRecoveryToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryToken, Long> {

    long countByContactTypeAndContactValueAndCreatedAtAfter(String contactType, String contactValue, OffsetDateTime createdAfter);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordRecoveryToken> findFirstByContactTypeAndContactValueAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            String contactType,
            String contactValue,
            OffsetDateTime now
    );
}
