package com.example.visceralmassageapi.memberships.controller;

import com.example.visceralmassageapi.memberships.dto.MembershipOfferResponse;
import com.example.visceralmassageapi.memberships.dto.MembershipOfferUpdateRequest;
import com.example.visceralmassageapi.memberships.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/memberships/offers")
@RequiredArgsConstructor
public class AdminMembershipOfferController {

    private final MembershipService membershipService;

    @GetMapping
    public List<MembershipOfferResponse> listOffers() {
        return membershipService.listAdminOffers();
    }

    @PutMapping("/{id}")
    public ResponseEntity<MembershipOfferResponse> updateOffer(
            @PathVariable long id,
            @Valid @RequestBody MembershipOfferUpdateRequest request
    ) {
        return ResponseEntity.ok(membershipService.updateOffer(id, request));
    }
}
