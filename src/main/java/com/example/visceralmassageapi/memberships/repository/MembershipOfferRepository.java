package com.example.visceralmassageapi.memberships.repository;

import com.example.visceralmassageapi.memberships.domain.MembershipOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipOfferRepository extends JpaRepository<MembershipOffer, Long> {
    List<MembershipOffer> findByActiveTrueOrderBySortOrderAscIdAsc();
}
