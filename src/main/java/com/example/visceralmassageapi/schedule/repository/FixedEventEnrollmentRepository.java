package com.example.visceralmassageapi.schedule.repository;

import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollment;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
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
              AND enrollment.status = com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus.ACTIVE
              AND event.startsAt < :to
              AND event.endsAt > :from
            """)
    List<FixedEventEnrollment> findActiveForUser(long userId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT enrollment
            FROM FixedEventEnrollment enrollment
            JOIN FETCH enrollment.event event
            JOIN FETCH event.service
            LEFT JOIN FETCH event.office
            JOIN FETCH enrollment.user
            WHERE event.specialist.id = :specialistId
              AND event.startsAt < :to
              AND event.endsAt > :from
            ORDER BY event.startsAt ASC, enrollment.createdAt ASC, enrollment.id ASC
            """)
    List<FixedEventEnrollment> findForSpecialistEvents(long specialistId, OffsetDateTime from, OffsetDateTime to);
}
