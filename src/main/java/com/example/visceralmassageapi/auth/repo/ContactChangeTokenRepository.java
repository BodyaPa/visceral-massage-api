package com.example.visceralmassageapi.auth.repo;

import com.example.visceralmassageapi.auth.domain.ContactChangeToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ContactChangeTokenRepository extends JpaRepository<ContactChangeToken, Long> {

    long countByUserIdAndContactTypeAndContactValueAndCreatedAtAfter(
            long userId,
            String contactType,
            String contactValue,
            OffsetDateTime createdAfter
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ContactChangeToken> findFirstByUserIdAndContactTypeAndContactValueAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            long userId,
            String contactType,
            String contactValue,
            OffsetDateTime now
    );
}
