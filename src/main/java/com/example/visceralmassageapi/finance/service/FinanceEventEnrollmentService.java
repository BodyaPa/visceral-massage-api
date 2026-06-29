package com.example.visceralmassageapi.finance.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.finance.dto.FinanceEventEnrollmentResponse;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.schedule.domain.FixedEvent;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollment;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;
import com.example.visceralmassageapi.schedule.repository.FixedEventEnrollmentRepository;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class FinanceEventEnrollmentService {

    private static final OffsetDateTime MIN_FINANCE_RANGE = OffsetDateTime.parse("1900-01-01T00:00:00Z");
    private static final OffsetDateTime MAX_FINANCE_RANGE = OffsetDateTime.parse("3000-01-01T00:00:00Z");

    private final FixedEventEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public Page<FinanceEventEnrollmentResponse> list(
            FixedEventEnrollmentStatus status,
            Long officeId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    ) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new BadRequestException("Range 'from' must be before 'to'");
        }
        return enrollmentRepository.findForFinance(status, officeId, effectiveFrom(from), effectiveTo(to), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public FinanceEventEnrollmentResponse confirmPayment(long enrollmentId, long actorId) {
        FixedEventEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Fixed event enrollment not found"));
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (enrollment.getStatus() != FixedEventEnrollmentStatus.ACTIVE) {
            throw new BadRequestException("Only active event enrollments can be payment-confirmed");
        }
        if (enrollment.getMembershipPurchase() != null) {
            throw new BadRequestException("Membership-paid event enrollments do not need manual payment confirmation");
        }
        if (enrollment.getPaymentConfirmedAt() != null) {
            throw new BadRequestException("Fixed event enrollment payment is already confirmed");
        }

        enrollment.setPaymentConfirmedAt(OffsetDateTime.now());
        enrollment.setPaymentConfirmedBy(actor);
        auditLogger.fixedEventEnrollmentPaymentConfirmed(enrollmentId, actorId);
        return toResponse(enrollment);
    }

    private FinanceEventEnrollmentResponse toResponse(FixedEventEnrollment enrollment) {
        FixedEvent event = enrollment.getEvent();
        ServiceOffering service = event.getService();
        User client = enrollment.getUser();
        User specialist = event.getSpecialist();
        Office office = event.getOffice();
        boolean paidWithMembership = enrollment.getMembershipPurchase() != null;

        return new FinanceEventEnrollmentResponse(
                enrollment.getId(),
                enrollment.getStatus(),
                client.getId(),
                displayName(client),
                client.getPhone() != null ? client.getPhone() : client.getEmail(),
                event.getId(),
                service.getId(),
                service.getTitleUa(),
                service.getTitleEn(),
                paidWithMembership ? null : service.getExternalPaymentUrl(),
                paidWithMembership ? enrollment.getMembershipPurchase().getId() : null,
                paidWithMembership,
                service.getBasePrice(),
                paidWithMembership || enrollment.getPaymentConfirmedAt() != null,
                enrollment.getPaymentConfirmedAt(),
                enrollment.getPaymentConfirmedBy() == null ? null : enrollment.getPaymentConfirmedBy().getId(),
                specialist.getId(),
                displayName(specialist),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                event.getStartsAt(),
                event.getEndsAt(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }

    private String displayName(User user) {
        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " " +
                (user.getLastName() == null ? "" : user.getLastName())).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return user.getPhone();
    }

    private OffsetDateTime effectiveFrom(OffsetDateTime from) {
        return from == null ? MIN_FINANCE_RANGE : from;
    }

    private OffsetDateTime effectiveTo(OffsetDateTime to) {
        return to == null ? MAX_FINANCE_RANGE : to;
    }
}
