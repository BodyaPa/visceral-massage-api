package com.example.visceralmassageapi.memberships.repository;

import com.example.visceralmassageapi.memberships.domain.MembershipPurchase;
import com.example.visceralmassageapi.memberships.domain.MembershipPurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface MembershipPurchaseRepository extends JpaRepository<MembershipPurchase, Long> {
    Page<MembershipPurchase> findByUserId(long userId, Pageable pageable);
    Page<MembershipPurchase> findByStatus(MembershipPurchaseStatus status, Pageable pageable);
    boolean existsByUserIdAndOfferIdAndStatus(long userId, long offerId, MembershipPurchaseStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MembershipPurchase> findByIdAndUserId(long id, long userId);
}
