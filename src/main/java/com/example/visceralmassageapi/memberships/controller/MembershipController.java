package com.example.visceralmassageapi.memberships.controller;

import com.example.visceralmassageapi.memberships.dto.MembershipOfferResponse;
import com.example.visceralmassageapi.memberships.dto.MembershipPaymentSessionResponse;
import com.example.visceralmassageapi.memberships.dto.MembershipPurchaseRequest;
import com.example.visceralmassageapi.memberships.dto.MembershipPurchaseResponse;
import com.example.visceralmassageapi.memberships.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/offers")
    public List<MembershipOfferResponse> listOffers() {
        return membershipService.listActiveOffers();
    }

    @GetMapping("/purchases/my")
    public Page<MembershipPurchaseResponse> listMyPurchases(Authentication authentication, Pageable pageable) {
        return membershipService.listMyPurchases(currentUserId(authentication), pageable);
    }

    @PostMapping("/purchases")
    public ResponseEntity<MembershipPurchaseResponse> createPurchase(
            Authentication authentication,
            @Valid @RequestBody MembershipPurchaseRequest request
    ) {
        return ResponseEntity.ok(membershipService.createPurchase(currentUserId(authentication), request));
    }

    @PostMapping("/purchases/{id}/payment-session")
    public ResponseEntity<MembershipPaymentSessionResponse> createPaymentSession(
            Authentication authentication,
            @PathVariable long id
    ) {
        return ResponseEntity.ok(membershipService.createPaymentSession(currentUserId(authentication), id));
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return userId;
    }
}
