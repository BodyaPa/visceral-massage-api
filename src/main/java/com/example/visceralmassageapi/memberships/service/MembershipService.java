package com.example.visceralmassageapi.memberships.service;

import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.memberships.domain.MembershipOffer;
import com.example.visceralmassageapi.memberships.domain.MembershipPurchase;
import com.example.visceralmassageapi.memberships.domain.MembershipPurchaseStatus;
import com.example.visceralmassageapi.memberships.dto.MembershipOfferResponse;
import com.example.visceralmassageapi.memberships.dto.MembershipOfferUpdateRequest;
import com.example.visceralmassageapi.memberships.dto.MembershipPaymentSessionResponse;
import com.example.visceralmassageapi.memberships.dto.MembershipPurchaseRequest;
import com.example.visceralmassageapi.memberships.dto.MembershipPurchaseResponse;
import com.example.visceralmassageapi.memberships.repository.MembershipOfferRepository;
import com.example.visceralmassageapi.memberships.repository.MembershipPurchaseRepository;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import com.example.visceralmassageapi.services.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipOfferRepository offerRepository;
    private final MembershipPurchaseRepository purchaseRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public List<MembershipOfferResponse> listAdminOffers() {
        return offerRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toOfferResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MembershipOfferResponse> listActiveOffers() {
        return offerRepository.findByActiveTrueOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toOfferResponse)
                .toList();
    }

    @Transactional
    public MembershipOfferResponse updateOffer(long id, MembershipOfferUpdateRequest request) {
        MembershipOffer offer = offerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Membership offer not found"));
        validateOfferUpdate(offer, request);

        offer.setTitleUa(trimRequired(request.titleUa()));
        offer.setTitleEn(trimToNull(request.titleEn()));
        offer.setDescriptionUa(trimToNull(request.descriptionUa()));
        offer.setDescriptionEn(trimToNull(request.descriptionEn()));
        offer.setPrice(request.price());
        offer.setVisitsTotal(request.visitsTotal());
        offer.setValidityDays(request.validityDays());
        offer.setActive(request.active());
        offer.getEligibleServiceIds().clear();
        offer.getEligibleServiceIds().addAll(resolveEligibleServiceIds(request.eligibleServiceIds()));

        return toOfferResponse(offer);
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

    @Transactional
    public MembershipPaymentSessionResponse createPaymentSession(long userId, long purchaseId) {
        MembershipPurchase purchase = purchaseRepository.findByIdAndUserId(purchaseId, userId)
                .orElseThrow(() -> new NotFoundException("Membership purchase not found"));
        if (purchase.getStatus() != MembershipPurchaseStatus.AWAITING_PAYMENT_CONFIRMATION) {
            throw new BadRequestException("Membership purchase is not awaiting payment");
        }

        return new MembershipPaymentSessionResponse(
                purchase.getId(),
                "MANUAL_REVIEW",
                null,
                true
        );
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
                offer.isActive(),
                Set.copyOf(offer.getEligibleServiceIds())
        );
    }

    private void validateOfferUpdate(MembershipOffer offer, MembershipOfferUpdateRequest request) {
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Membership offer price must be zero or greater");
        }
        if (request.validityDays() < 1) {
            throw new BadRequestException("Membership offer validity must be at least one day");
        }
        if (offer.getKind().name().equals("MEMBERSHIP") && (request.visitsTotal() == null || request.visitsTotal() < 1)) {
            throw new BadRequestException("Membership offer visits must be greater than zero");
        }
        if (request.visitsTotal() != null && request.visitsTotal() < 0) {
            throw new BadRequestException("Membership offer visits must be zero or greater");
        }
    }

    private Set<Long> resolveEligibleServiceIds(Set<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> normalizedIds = requestedIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> existingIds = serviceOfferingRepository.findAllById(normalizedIds)
                .stream()
                .map(ServiceOffering::getId)
                .collect(Collectors.toSet());
        if (existingIds.size() != normalizedIds.size()) {
            throw new BadRequestException("Membership offer references unknown services");
        }

        return normalizedIds;
    }

    private String trimRequired(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Membership offer title is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public MembershipPurchase consumeVisit(long userId, Long purchaseId, ServiceOffering service) {
        if (purchaseId == null) {
            return null;
        }

        MembershipPurchase purchase = purchaseRepository.findByIdAndUserId(purchaseId, userId)
                .orElseThrow(() -> new NotFoundException("Membership purchase not found"));

        if (purchase.getStatus() != MembershipPurchaseStatus.ACTIVE) {
            throw new BadRequestException("Membership purchase is not active");
        }
        if (purchase.getExpiresAt() != null && !purchase.getExpiresAt().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Membership purchase is expired");
        }
        if (purchase.getVisitsRemaining() == null || purchase.getVisitsRemaining() <= 0) {
            throw new BadRequestException("Membership purchase has no remaining visits");
        }

        Set<Long> eligibleServiceIds = purchase.getOffer().getEligibleServiceIds();
        if (!eligibleServiceIds.contains(service.getId())) {
            throw new BadRequestException("Membership purchase is not eligible for selected service");
        }

        purchase.setVisitsRemaining(purchase.getVisitsRemaining() - 1);
        return purchase;
    }

    @Transactional
    public void refundVisit(MembershipPurchase purchase) {
        if (purchase == null || purchase.getVisitsRemaining() == null) {
            return;
        }
        if (purchase.getVisitsTotal() != null && purchase.getVisitsRemaining() >= purchase.getVisitsTotal()) {
            return;
        }
        purchase.setVisitsRemaining(purchase.getVisitsRemaining() + 1);
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
