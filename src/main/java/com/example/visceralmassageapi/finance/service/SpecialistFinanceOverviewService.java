package com.example.visceralmassageapi.finance.service;

import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.domain.SpecialistPayoutStatus;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.finance.dto.SpecialistFinanceOverviewResponse;
import com.example.visceralmassageapi.finance.repository.SpecialistFinanceSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.example.visceralmassageapi.booking.repository.BookingSpecifications.specialistFinanceFilter;

@Service
@RequiredArgsConstructor
public class SpecialistFinanceOverviewService {

    private static final long MAX_QUERY_DAYS = 93;

    private final BookingRepository bookingRepository;
    private final SpecialistFinanceSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public SpecialistFinanceOverviewResponse getOverview(long specialistId, OffsetDateTime from, OffsetDateTime to) {
        validateRange(from, to);
        requireSpecialist(specialistId);

        BigDecimal sharePercent = settingsRepository.findById(specialistId)
                .map(settings -> settings.getSpecialistSharePercent())
                .orElse(BigDecimal.ZERO);
        List<Booking> completedBookings = bookingRepository.findAll(
                specialistFinanceFilter(specialistId, BookingStatus.CONFIRMED, from, to)
        );
        List<Booking> pendingBookings = bookingRepository.findAll(
                specialistFinanceFilter(specialistId, BookingStatus.AWAITING_PAYMENT_CONFIRMATION, from, to)
        );

        BigDecimal grossIncome = sumBookedPrice(completedBookings);
        BigDecimal pendingGrossIncome = sumBookedPrice(pendingBookings);

        return new SpecialistFinanceOverviewResponse(
                completedBookings.size(),
                pendingBookings.size(),
                completedBookings.stream().filter(booking -> booking.getSpecialistPayoutStatus() == SpecialistPayoutStatus.PENDING).count(),
                completedBookings.stream().filter(booking -> booking.getSpecialistPayoutStatus() == SpecialistPayoutStatus.PAID).count(),
                workedMinutes(completedBookings),
                grossIncome,
                sumSpecialistShare(completedBookings, sharePercent),
                sumSpecialistShare(
                        completedBookings.stream()
                                .filter(booking -> booking.getSpecialistPayoutStatus() == SpecialistPayoutStatus.PENDING)
                                .toList(),
                        sharePercent
                ),
                sumSpecialistShare(
                        completedBookings.stream()
                                .filter(booking -> booking.getSpecialistPayoutStatus() == SpecialistPayoutStatus.PAID)
                                .toList(),
                        sharePercent
                ),
                pendingGrossIncome,
                sumSpecialistShare(pendingBookings, sharePercent),
                sharePercent
        );
    }

    private void requireSpecialist(long specialistId) {
        var specialist = userRepository.findById(specialistId)
                .orElseThrow(() -> new NotFoundException("Specialist not found"));

        if (!specialist.getRoles().contains(UserRole.SPECIALIST)) {
            throw new AccessDeniedException("Specialist role is required");
        }
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("Booking range is invalid");
        }

        if (from != null && to != null && ChronoUnit.DAYS.between(from, to) > MAX_QUERY_DAYS) {
            throw new BadRequestException("Booking range is too large");
        }
    }

    private BigDecimal sumBookedPrice(List<Booking> bookings) {
        return bookings.stream()
                .map(Booking::getBookedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumSpecialistShare(List<Booking> bookings, BigDecimal sharePercent) {
        return bookings.stream()
                .map(booking -> FinanceShareCalculator.specialistShare(booking.getBookedPrice(), sharePercent))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long workedMinutes(List<Booking> bookings) {
        return bookings.stream()
                .mapToLong(booking -> Duration.between(booking.getStartsAt(), booking.getEndsAt()).toMinutes())
                .sum();
    }
}
