package com.example.visceralmassageapi.memberships.controller;

import com.example.visceralmassageapi.memberships.domain.MembershipPurchaseStatus;
import com.example.visceralmassageapi.memberships.dto.MembershipPurchaseResponse;
import com.example.visceralmassageapi.memberships.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/finance/memberships")
@RequiredArgsConstructor
public class AdminFinanceMembershipController {

    private final MembershipService membershipService;

    @GetMapping
    public Page<MembershipPurchaseResponse> list(
            @RequestParam(required = false) MembershipPurchaseStatus status,
            Pageable pageable
    ) {
        return membershipService.listFinancePurchases(status, pageable);
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<MembershipPurchaseResponse> confirmPayment(Authentication authentication, @PathVariable long id) {
        return ResponseEntity.ok(membershipService.confirmPayment(currentUserId(authentication), id));
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return userId;
    }
}
