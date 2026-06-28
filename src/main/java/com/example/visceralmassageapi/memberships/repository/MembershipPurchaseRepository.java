package com.example.visceralmassageapi.memberships.repository;

import com.example.visceralmassageapi.memberships.domain.MembershipPurchase;
import com.example.visceralmassageapi.memberships.domain.MembershipPurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPurchaseRepository extends JpaRepository<MembershipPurchase, Long> {
    Page<MembershipPurchase> findByUserId(long userId, Pageable pageable);
    Page<MembershipPurchase> findByStatus(MembershipPurchaseStatus status, Pageable pageable);
    boolean existsByUserIdAndOfferIdAndStatus(long userId, long offerId, MembershipPurchaseStatus status);
}
