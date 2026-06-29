package com.example.visceralmassageapi.schedule.repository;

import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollment;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FixedEventEnrollmentRepository extends JpaRepository<FixedEventEnrollment, Long> {

    long countByEventIdAndStatus(long eventId, FixedEventEnrollmentStatus status);

    Optional<FixedEventEnrollment> findByEventIdAndUserIdAndStatus(
            long eventId,
            long userId,
            FixedEventEnrollmentStatus status
    );

    Optional<FixedEventEnrollment> findFirstByEventIdAndUserIdOrderByUpdatedAtDescIdDesc(long eventId, long userId);

    @Query("""
            SELECT enrollment
            FROM FixedEventEnrollment enrollment
            WHERE enrollment.event.id IN :eventIds
              AND enrollment.status = com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus.ACTIVE
            """)
    List<FixedEventEnrollment> findActiveForEvents(Collection<Long> eventIds);

    @Query("""
            SELECT enrollment
            FROM FixedEventEnrollment enrollment
            JOIN FETCH enrollment.event event
            WHERE enrollment.user.id = :userId
              AND event.startsAt < :to
              AND event.endsAt > :from
            ORDER BY event.startsAt DESC, enrollment.updatedAt DESC, enrollment.id DESC
            """)
    List<FixedEventEnrollment> findForUser(long userId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT enrollment
            FROM FixedEventEnrollment enrollment
            JOIN FETCH enrollment.event event
            JOIN FETCH event.service
            LEFT JOIN FETCH event.office office
            JOIN FETCH enrollment.user
            WHERE (:specialistId IS NULL OR event.specialist.id = :specialistId)
              AND event.startsAt < :to
              AND event.endsAt > :from
              AND (:eventActive IS NULL OR event.active = :eventActive)
              AND (:status IS NULL OR enrollment.status = :status)
              AND (:officeId IS NULL OR office.id = :officeId)
              AND (:serviceId IS NULL OR event.service.id = :serviceId)
            ORDER BY event.startsAt ASC, enrollment.createdAt ASC, enrollment.id ASC
            """)
    List<FixedEventEnrollment> findForSpecialistEvents(
            Long specialistId,
            OffsetDateTime from,
            OffsetDateTime to,
            Boolean eventActive,
            FixedEventEnrollmentStatus status,
            Long officeId,
            Long serviceId
    );

    @Query(value = """
            SELECT enrollment
            FROM FixedEventEnrollment enrollment
            JOIN FETCH enrollment.user
            JOIN FETCH enrollment.event event
            JOIN FETCH event.service
            JOIN FETCH event.specialist
            LEFT JOIN FETCH event.office office
            LEFT JOIN FETCH enrollment.membershipPurchase
            WHERE (:status IS NULL OR enrollment.status = :status)
              AND (:officeId IS NULL OR office.id = :officeId)
              AND event.endsAt > :from
              AND event.startsAt < :to
            """,
            countQuery = """
            SELECT COUNT(enrollment)
            FROM FixedEventEnrollment enrollment
            JOIN enrollment.event event
            LEFT JOIN event.office office
            WHERE (:status IS NULL OR enrollment.status = :status)
              AND (:officeId IS NULL OR office.id = :officeId)
              AND event.endsAt > :from
              AND event.startsAt < :to
            """)
    Page<FixedEventEnrollment> findForFinance(
            FixedEventEnrollmentStatus status,
            Long officeId,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(enrollment)
            FROM FixedEventEnrollment enrollment
            JOIN enrollment.event event
            LEFT JOIN event.office office
            WHERE enrollment.status = com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus.ACTIVE
              AND enrollment.membershipPurchase IS NULL
              AND enrollment.paymentConfirmedAt IS NULL
              AND (:officeId IS NULL OR office.id = :officeId)
              AND event.endsAt > :from
              AND event.startsAt < :to
            """)
    long countPendingPaymentForFinance(Long officeId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT COUNT(enrollment)
            FROM FixedEventEnrollment enrollment
            JOIN enrollment.event event
            LEFT JOIN event.office office
            WHERE enrollment.status = com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus.ACTIVE
              AND (enrollment.membershipPurchase IS NOT NULL OR enrollment.paymentConfirmedAt IS NOT NULL)
              AND (:officeId IS NULL OR office.id = :officeId)
              AND event.endsAt > :from
              AND event.startsAt < :to
            """)
    long countConfirmedPaymentForFinance(Long officeId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT COALESCE(SUM(event.service.basePrice), 0)
            FROM FixedEventEnrollment enrollment
            JOIN enrollment.event event
            LEFT JOIN event.office office
            WHERE enrollment.status = com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus.ACTIVE
              AND enrollment.paymentConfirmedAt IS NOT NULL
              AND (:officeId IS NULL OR office.id = :officeId)
              AND event.endsAt > :from
              AND event.startsAt < :to
            """)
    BigDecimal sumConfirmedPaymentAmountForFinance(Long officeId, OffsetDateTime from, OffsetDateTime to);

    default List<FixedEventEnrollment> findForSpecialistEvents(Long specialistId, OffsetDateTime from, OffsetDateTime to) {
        return findForSpecialistEvents(specialistId, from, to, null, null, null, null);
    }

    @Query("""
            SELECT enrollment
            FROM FixedEventEnrollment enrollment
            JOIN FETCH enrollment.user
            JOIN FETCH enrollment.event event
            JOIN FETCH event.service
            LEFT JOIN FETCH event.office
            WHERE enrollment.reminderOptIn = true
              AND enrollment.reminderSentAt IS NULL
              AND enrollment.status = com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus.ACTIVE
              AND event.active = true
              AND event.startsAt > :now
              AND event.startsAt <= :remindBefore
            ORDER BY event.startsAt ASC, enrollment.id ASC
            """)
    List<FixedEventEnrollment> findDueEventReminders(OffsetDateTime now, OffsetDateTime remindBefore);
}
