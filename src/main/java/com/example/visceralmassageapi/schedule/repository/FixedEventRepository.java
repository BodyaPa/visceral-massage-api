package com.example.visceralmassageapi.schedule.repository;

import com.example.visceralmassageapi.schedule.domain.FixedEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface FixedEventRepository extends JpaRepository<FixedEvent, Long> {

    @Query("""
            SELECT event
            FROM FixedEvent event
            JOIN FETCH event.service service
            JOIN FETCH event.specialist specialist
            LEFT JOIN FETCH event.office office
            WHERE event.active = true
              AND service.active = true
              AND event.startsAt < :to
              AND event.endsAt > :from
              AND (office IS NULL OR office.active = true)
              AND (:officeId IS NULL OR office.id = :officeId)
              AND (:specialistId IS NULL OR specialist.id = :specialistId)
              AND (:serviceId IS NULL OR service.id = :serviceId)
            ORDER BY event.startsAt ASC, event.id ASC
            """)
    List<FixedEvent> findPublicRange(OffsetDateTime from, OffsetDateTime to, Long officeId, Long specialistId, Long serviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT event
            FROM FixedEvent event
            JOIN FETCH event.service
            JOIN FETCH event.specialist
            LEFT JOIN FETCH event.office
            WHERE event.id = :id
            """)
    Optional<FixedEvent> findForEnrollment(long id);

    @Query("""
            SELECT event
            FROM FixedEvent event
            JOIN FETCH event.service
            JOIN FETCH event.specialist
            LEFT JOIN FETCH event.office
            WHERE event.specialist.id = :specialistId
              AND event.startsAt < :to
              AND event.endsAt > :from
            ORDER BY event.startsAt ASC, event.id ASC
            """)
    List<FixedEvent> findOwnRange(long specialistId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT event
            FROM FixedEvent event
            JOIN FETCH event.service
            JOIN FETCH event.specialist specialist
            LEFT JOIN FETCH event.office
            WHERE (:specialistId IS NULL OR specialist.id = :specialistId)
              AND event.startsAt < :to
              AND event.endsAt > :from
              AND (:active IS NULL OR event.active = :active)
              AND (:officeId IS NULL OR office.id = :officeId)
              AND (:serviceId IS NULL OR event.service.id = :serviceId)
            ORDER BY event.startsAt ASC, event.id ASC
            """)
    List<FixedEvent> findManagedRange(
            Long specialistId,
            OffsetDateTime from,
            OffsetDateTime to,
            Boolean active,
            Long officeId,
            Long serviceId
    );

    default List<FixedEvent> findManagedRange(Long specialistId, OffsetDateTime from, OffsetDateTime to) {
        return findManagedRange(specialistId, from, to, null, null, null);
    }

    @Query("""
            SELECT event
            FROM FixedEvent event
            WHERE event.specialist.id = :specialistId
              AND event.active = true
              AND (:excludedId IS NULL OR event.id <> :excludedId)
              AND event.startsAt < :endsAt
              AND event.endsAt > :startsAt
            ORDER BY event.startsAt ASC, event.id ASC
            """)
    List<FixedEvent> findActiveOverlappingForSpecialist(
            long specialistId,
            Long excludedId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    );

    @Query("""
            SELECT COUNT(event) > 0
            FROM FixedEvent event
            WHERE event.specialist.id = :specialistId
              AND event.active = true
              AND (:excludedId IS NULL OR event.id <> :excludedId)
              AND event.startsAt < :endsAt
              AND event.endsAt > :startsAt
            """)
    boolean overlapsActiveForSpecialist(long specialistId, Long excludedId, OffsetDateTime startsAt, OffsetDateTime endsAt);

    @Query("""
            SELECT event
            FROM FixedEvent event
            WHERE event.active = true
              AND event.endsAt <= :now
              AND NOT EXISTS (
                  SELECT enrollment.id
                  FROM FixedEventEnrollment enrollment
                  WHERE enrollment.event = event
                    AND enrollment.status = com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus.ACTIVE
              )
            """)
    List<FixedEvent> findPastActiveEventsWithoutActiveEnrollments(OffsetDateTime now);
}
