package com.example.visceralmassageapi.booking.repository;

import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {
    boolean existsByAvailabilityBlockIdAndStatusNot(long availabilityBlockId, BookingStatus status);

    long countByAvailabilityBlockIdAndStatusNot(long availabilityBlockId, BookingStatus status);

    boolean existsByAvailabilityBlockId(long availabilityBlockId);

    @Query("""
            SELECT COUNT(booking) > 0
            FROM Booking booking
            WHERE booking.availabilityBlock.id = :availabilityBlockId
              AND booking.status <> com.example.visceralmassageapi.booking.domain.BookingStatus.CANCELLED
              AND booking.startsAt < :endsAt
              AND booking.endsAt > :startsAt
            """)
    boolean existsActiveOverlappingBooking(long availabilityBlockId, OffsetDateTime startsAt, OffsetDateTime endsAt);

    @Query("""
            SELECT COUNT(booking) > 0
            FROM Booking booking
            WHERE booking.specialist.id = :specialistId
              AND booking.status <> com.example.visceralmassageapi.booking.domain.BookingStatus.CANCELLED
              AND booking.startsAt < :endsAt
              AND booking.endsAt > :startsAt
            """)
    boolean existsActiveOverlappingSpecialistBooking(long specialistId, OffsetDateTime startsAt, OffsetDateTime endsAt);

    @Query("""
            SELECT booking
            FROM Booking booking
            WHERE booking.availabilityBlock.id = :availabilityBlockId
              AND booking.status <> com.example.visceralmassageapi.booking.domain.BookingStatus.CANCELLED
              AND booking.startsAt < :to
              AND booking.endsAt > :from
            ORDER BY booking.startsAt ASC, booking.id ASC
            """)
    List<Booking> findActiveBookingsForAvailabilityBlock(long availabilityBlockId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT booking.availabilityBlock.id
            FROM Booking booking
            WHERE booking.specialist.id = :specialistId
              AND booking.status <> com.example.visceralmassageapi.booking.domain.BookingStatus.CANCELLED
              AND booking.startsAt < :to
              AND booking.endsAt > :from
            """)
    List<Long> findBookedAvailabilityBlockIds(long specialistId, OffsetDateTime from, OffsetDateTime to);

    @Query(value = """
            SELECT booking
            FROM Booking booking
            JOIN FETCH booking.specialist
            JOIN FETCH booking.service
            LEFT JOIN FETCH booking.office
            WHERE booking.user.id = :userId
            """,
            countQuery = """
            SELECT COUNT(booking)
            FROM Booking booking
            WHERE booking.user.id = :userId
            """)
    Page<Booking> findUserBookings(long userId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "specialist", "service", "office"})
    Page<Booking> findAll(Specification<Booking> specification, Pageable pageable);

    @Query("""
            SELECT booking
            FROM Booking booking
            JOIN FETCH booking.user
            JOIN FETCH booking.service
            LEFT JOIN FETCH booking.office
            WHERE booking.specialist.id = :specialistId
              AND booking.status <> com.example.visceralmassageapi.booking.domain.BookingStatus.CANCELLED
              AND booking.startsAt < :to
              AND booking.endsAt > :from
            ORDER BY booking.startsAt ASC, booking.id ASC
            """)
    List<Booking> findSpecialistBookings(long specialistId, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT booking
            FROM Booking booking
            JOIN FETCH booking.specialist
            LEFT JOIN FETCH booking.office
            WHERE booking.status <> com.example.visceralmassageapi.booking.domain.BookingStatus.CANCELLED
              AND booking.startsAt < :to
              AND booking.endsAt > :from
              AND (:officeId IS NULL OR booking.office.id = :officeId)
              AND (:specialistId IS NULL OR booking.specialist.id = :specialistId)
            ORDER BY booking.startsAt ASC, booking.id ASC
            """)
    List<Booking> findPublicOccupiedBookings(OffsetDateTime from, OffsetDateTime to, Long officeId, Long specialistId);
}
