package com.example.visceralmassageapi.schedule.repository;

import com.example.visceralmassageapi.schedule.domain.SpecialistAvailabilityBlock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SpecialistAvailabilityBlockRepository extends JpaRepository<SpecialistAvailabilityBlock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT block
            FROM SpecialistAvailabilityBlock block
            JOIN FETCH block.specialist
            LEFT JOIN FETCH block.office
            LEFT JOIN FETCH block.service
            WHERE block.id = :id
            """)
    Optional<SpecialistAvailabilityBlock> findForBooking(long id);

    @Query("""
            SELECT block
            FROM SpecialistAvailabilityBlock block
            JOIN FETCH block.specialist
            LEFT JOIN FETCH block.office
            LEFT JOIN FETCH block.service
            WHERE block.specialist.id = :specialistId
              AND block.startsAt < :to
              AND block.endsAt > :from
            ORDER BY block.startsAt ASC, block.id ASC
            """)
    List<SpecialistAvailabilityBlock> findOwnRange(long specialistId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT block
            FROM SpecialistAvailabilityBlock block
            JOIN FETCH block.specialist specialist
            LEFT JOIN FETCH block.office
            LEFT JOIN FETCH block.service
            WHERE (:specialistId IS NULL OR specialist.id = :specialistId)
              AND block.startsAt < :to
              AND block.endsAt > :from
            ORDER BY block.startsAt ASC, block.id ASC
            """)
    List<SpecialistAvailabilityBlock> findManagedRange(Long specialistId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT block
            FROM SpecialistAvailabilityBlock block
            JOIN FETCH block.specialist specialist
            LEFT JOIN FETCH block.office office
            LEFT JOIN FETCH block.service service
            WHERE block.status = com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus.AVAILABLE
              AND block.startsAt < :to
              AND block.endsAt > :from
              AND (:officeId IS NULL OR office.id = :officeId)
              AND (:specialistId IS NULL OR specialist.id = :specialistId)
              AND NOT EXISTS (
                  SELECT booking.id
                  FROM Booking booking
                  WHERE booking.availabilityBlock = block
                    AND booking.status <> com.example.visceralmassageapi.booking.domain.BookingStatus.CANCELLED
              )
            ORDER BY block.startsAt ASC, block.id ASC
            """)
    List<SpecialistAvailabilityBlock> findPublicAvailability(
            OffsetDateTime from,
            OffsetDateTime to,
            Long officeId,
            Long specialistId
    );

    @Query("""
            SELECT block
            FROM SpecialistAvailabilityBlock block
            JOIN FETCH block.specialist specialist
            LEFT JOIN FETCH block.office office
            LEFT JOIN FETCH block.service service
            WHERE block.status = com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus.AVAILABLE
              AND block.startsAt < :to
              AND block.endsAt > :from
              AND (:officeId IS NULL OR office.id = :officeId)
              AND (:specialistId IS NULL OR specialist.id = :specialistId)
            ORDER BY block.startsAt ASC, block.id ASC
            """)
    List<SpecialistAvailabilityBlock> findPublicAvailabilityBlocks(
            OffsetDateTime from,
            OffsetDateTime to,
            Long officeId,
            Long specialistId
    );

    @Query("""
            SELECT block
            FROM SpecialistAvailabilityBlock block
            JOIN FETCH block.specialist specialist
            LEFT JOIN FETCH block.office office
            LEFT JOIN FETCH block.service service
            WHERE block.status = com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus.BLOCKED
              AND block.startsAt < :to
              AND block.endsAt > :from
              AND (:officeId IS NULL OR office.id = :officeId)
              AND (:specialistId IS NULL OR specialist.id = :specialistId)
            ORDER BY block.startsAt ASC, block.id ASC
            """)
    List<SpecialistAvailabilityBlock> findPublicBlockedRange(
            OffsetDateTime from,
            OffsetDateTime to,
            Long officeId,
            Long specialistId
    );

    @Query("""
            SELECT COUNT(block) > 0
            FROM SpecialistAvailabilityBlock block
            WHERE block.specialist.id = :specialistId
              AND (:excludedId IS NULL OR block.id <> :excludedId)
              AND block.startsAt < :endsAt
              AND block.endsAt > :startsAt
            """)
    boolean overlaps(long specialistId, Long excludedId, OffsetDateTime startsAt, OffsetDateTime endsAt);

    @Query("""
            SELECT COUNT(block) > 0
            FROM SpecialistAvailabilityBlock block
            WHERE block.specialist.id = :specialistId
              AND block.status = :status
              AND (:excludedId IS NULL OR block.id <> :excludedId)
              AND block.startsAt < :endsAt
              AND block.endsAt > :startsAt
            """)
    boolean overlapsStatus(
            long specialistId,
            com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus status,
            Long excludedId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    );

    @Query("""
            SELECT COUNT(block) > 0
            FROM SpecialistAvailabilityBlock block
            WHERE block.specialist.id = :specialistId
              AND block.status = com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus.BLOCKED
              AND block.startsAt < :endsAt
              AND block.endsAt > :startsAt
            """)
    boolean overlapsBlocked(long specialistId, OffsetDateTime startsAt, OffsetDateTime endsAt);

    @Query("""
            SELECT COUNT(block) > 0
            FROM SpecialistAvailabilityBlock block
            LEFT JOIN block.office office
            WHERE block.specialist.id = :specialistId
              AND block.status = com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus.BLOCKED
              AND block.startsAt < :endsAt
              AND block.endsAt > :startsAt
              AND (office IS NULL OR :officeId IS NULL OR office.id = :officeId)
            """)
    boolean overlapsBlockedForAvailability(
            long specialistId,
            Long officeId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    );

    @Query("""
            SELECT block
            FROM SpecialistAvailabilityBlock block
            LEFT JOIN FETCH block.office office
            LEFT JOIN FETCH block.service service
            WHERE block.specialist.id = :specialistId
              AND block.status = com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus.BLOCKED
              AND block.startsAt < :to
              AND block.endsAt > :from
              AND (office IS NULL OR :officeId IS NULL OR office.id = :officeId)
            ORDER BY block.startsAt ASC, block.id ASC
            """)
    List<SpecialistAvailabilityBlock> findBlockedForAvailability(
            long specialistId,
            Long officeId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
