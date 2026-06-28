package com.example.visceralmassageapi.memberships.service;

import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.memberships.domain.MembershipOffer;
import com.example.visceralmassageapi.memberships.domain.MembershipPurchase;
import com.example.visceralmassageapi.memberships.domain.MembershipPurchaseStatus;
import com.example.visceralmassageapi.memberships.dto.MembershipOfferResponse;
import com.example.visceralmassageapi.memberships.dto.MembershipPurchaseRequest;
import com.example.visceralmassageapi.memberships.dto.MembershipPurchaseResponse;
import com.example.visceralmassageapi.memberships.repository.MembershipOfferRepository;
import com.example.visceralmassageapi.memberships.repository.MembershipPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipOfferRepository offerRepository;
    private final MembershipPurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public List<MembershipOfferResponse> listActiveOffers() {
        return offerRepository.findByActiveTrueOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toOfferResponse)
                .toList();
    }

    @Transactional
    public MembershipPurchaseResponse createPurchase(long userId, MembershipPurchaseRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        var offer = offerRepository.findById(request.offerId())
                .orElseThrow(() -> new NotFoundException("Membership offer not found"));

        if (!offer.isActive()) {
            throw new BadRequestException("Membership offer is inactive");
        }

        if (purchaseRepository.existsByUserIdAndOfferIdAndStatus(userId, offer.getId(), MembershipPurchaseStatus.AWAITING_PAYMENT_CONFIRMATION)) {
            throw new BadRequestException("Membership purchase is already awaiting payment confirmation");
        }

        MembershipPurchase purchase = new MembershipPurchase();
        purchase.setUser(user);
        purchase.setOffer(offer);
        purchase.setStatus(MembershipPurchaseStatus.AWAITING_PAYMENT_CONFIRMATION);
        purchase.setPriceSnapshot(offer.getPrice());
        purchase.setVisitsTotal(offer.getVisitsTotal());
        purchase.setVisitsRemaining(offer.getVisitsTotal());

        MembershipPurchase saved = purchaseRepository.save(purchase);
        auditLogger.membershipPurchaseCreated(saved.getId(), userId);
        return toPurchaseResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<MembershipPurchaseResponse> listMyPurchases(long userId, Pageable pageable) {
        return purchaseRepository.findByUserId(userId, pageable).map(this::toPurchaseResponse);
    }

    @Transactional(readOnly = true)
    public Page<MembershipPurchaseResponse> listFinancePurchases(MembershipPurchaseStatus status, Pageable pageable) {
        Page<MembershipPurchase> purchases = status == null
                ? purchaseRepository.findAll(pageable)
                : purchaseRepository.findByStatus(status, pageable);
        return purchases.map(this::toPurchaseResponse);
    }

    @Transactional
    public MembershipPurchaseResponse confirmPayment(long actorId, long purchaseId) {
        var actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        var purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Membership purchase not found"));

        if (purchase.getStatus() != MembershipPurchaseStatus.AWAITING_PAYMENT_CONFIRMATION) {
            throw new BadRequestException("Membership purchase is not awaiting payment confirmation");
        }

        OffsetDateTime now = OffsetDateTime.now();
        purchase.setStatus(MembershipPurchaseStatus.ACTIVE);
        purchase.setActivatedAt(now);
        purchase.setExpiresAt(now.plusDays(purchase.getOffer().getValidityDays()));
        purchase.setConfirmedBy(actor);
        auditLogger.membershipPurchasePaymentConfirmed(purchaseId, actorId);
        return toPurchaseResponse(purchase);
    }

    private MembershipOfferResponse toOfferResponse(MembershipOffer offer) {
        return new MembershipOfferResponse(
                offer.getId(),
                offer.getCode(),
                offer.getKind(),
                offer.getTitleUa(),
                offer.getTitleEn(),
                offer.getDescriptionUa(),
                offer.getDescriptionEn(),
                offer.getPrice(),
                offer.getVisitsTotal(),
                offer.getValidityDays(),
                offer.isActive()
        );
    }

    private MembershipPurchaseResponse toPurchaseResponse(MembershipPurchase purchase) {
        var offer = purchase.getOffer();
        var confirmedBy = purchase.getConfirmedBy();
        return new MembershipPurchaseResponse(
                purchase.getId(),
                purchase.getStatus(),
                purchase.getUser().getId(),
                offer.getId(),
                offer.getCode(),
                offer.getKind(),
                offer.getTitleUa(),
                offer.getTitleEn(),
                purchase.getPriceSnapshot(),
                purchase.getVisitsTotal(),
                purchase.getVisitsRemaining(),
                purchase.getActivatedAt(),
                purchase.getExpiresAt(),
                confirmedBy == null ? null : confirmedBy.getId(),
                purchase.getCreatedAt(),
                purchase.getUpdatedAt()
        );
    }
}
