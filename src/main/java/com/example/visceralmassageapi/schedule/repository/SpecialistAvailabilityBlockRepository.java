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
            WHERE block.id = :id
            """)
    Optional<SpecialistAvailabilityBlock> findForBooking(long id);

    @Query("""
            SELECT block
            FROM SpecialistAvailabilityBlock block
            LEFT JOIN FETCH block.office
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
            LEFT JOIN FETCH block.office office
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
            SELECT COUNT(block) > 0
            FROM SpecialistAvailabilityBlock block
            WHERE block.specialist.id = :specialistId
              AND (:excludedId IS NULL OR block.id <> :excludedId)
              AND block.startsAt < :endsAt
              AND block.endsAt > :startsAt
            """)
    boolean overlaps(long specialistId, Long excludedId, OffsetDateTime startsAt, OffsetDateTime endsAt);
}
