package com.example.visceralmassageapi.booking.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.domain.SpecialistPayoutStatus;
import com.example.visceralmassageapi.booking.dto.BookingRequest;
import com.example.visceralmassageapi.booking.dto.BookingResponse;
import com.example.visceralmassageapi.booking.dto.FinanceBookingResponse;
import com.example.visceralmassageapi.booking.dto.ManualBookingRequest;
import com.example.visceralmassageapi.booking.dto.SpecialistBookingResponse;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.finance.repository.SpecialistFinanceSettingsRepository;
import com.example.visceralmassageapi.finance.service.FinanceShareCalculator;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.schedule.domain.ScheduleBlockType;
import com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus;
import com.example.visceralmassageapi.schedule.domain.SpecialistAvailabilityBlock;
import com.example.visceralmassageapi.schedule.repository.FixedEventRepository;
import com.example.visceralmassageapi.schedule.repository.SpecialistAvailabilityBlockRepository;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import com.example.visceralmassageapi.services.entity.ServiceBookingMode;
import com.example.visceralmassageapi.services.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import static com.example.visceralmassageapi.booking.repository.BookingSpecifications.financeFilter;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final long MAX_SPECIALIST_QUERY_DAYS = 93;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SpecialistAvailabilityBlockRepository availabilityBlockRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final AuditLogger auditLogger;
    private final FixedEventRepository fixedEventRepository;
    private final SpecialistFinanceSettingsRepository specialistFinanceSettingsRepository;

    @Transactional
    public BookingResponse create(long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return toResponse(createBooking(
                user,
                request.availabilityBlockId(),
                request.serviceId(),
                request.startsAt(),
                request.reminderOptIn(),
                null
        ));
    }

    @Transactional
    public SpecialistBookingResponse createManual(long actorId, ManualBookingRequest request) {
        Long allowedSpecialistId = resolveManagedSpecialistId(actorId, request.specialistId());
        User client = findActiveClient(request.clientIdentifier());
        Booking booking = createBooking(
                client,
                request.availabilityBlockId(),
                request.serviceId(),
                request.startsAt(),
                request.reminderOptIn(),
                allowedSpecialistId
        );
        return toSpecialistResponse(booking);
    }

    private Booking createBooking(
            User client,
            long availabilityBlockId,
            long serviceId,
            OffsetDateTime requestedStartsAt,
            boolean reminderOptIn,
            Long requiredSpecialistId
    ) {
        SpecialistAvailabilityBlock block = availabilityBlockRepository.findForBooking(availabilityBlockId)
                .orElseThrow(() -> new NotFoundException("Availability block not found"));
        ServiceOffering service = serviceOfferingRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found"));

        if (requiredSpecialistId != null && !block.getSpecialist().getId().equals(requiredSpecialistId)) {
            throw new NotFoundException("Availability block not found");
        }

        if (!service.isActive()) {
            throw new BadRequestException("Service is inactive");
        }

        if (service.getBookingMode() != ServiceBookingMode.INDIVIDUAL_APPOINTMENT) {
            throw new BadRequestException("Fixed events must be booked through event enrollment");
        }

        if (block.getStatus() != ScheduleBlockStatus.AVAILABLE) {
            throw new BadRequestException("Availability block is not available");
        }

        if (block.getItemType() == ScheduleBlockType.APPOINTMENT_SLOT
                && (block.getService() == null || !block.getService().getId().equals(service.getId()))) {
            throw new BadRequestException("Appointment slot is assigned to another service");
        }

        if (block.getOffice() != null && !block.getOffice().isActive()) {
            throw new BadRequestException("Availability block office is inactive");
        }

        OffsetDateTime bookingStartsAt = block.getItemType() == ScheduleBlockType.APPOINTMENT_SLOT
                ? block.getStartsAt()
                : requestedStartsAt == null ? block.getStartsAt() : requestedStartsAt;
        OffsetDateTime bookingEndsAt = bookingStartsAt.plusMinutes(service.getDurationMinutes());

        if (bookingStartsAt.isBefore(block.getStartsAt()) || bookingEndsAt.isAfter(block.getEndsAt())) {
            throw new BadRequestException("Availability block is shorter than the selected service");
        }

        if (!bookingStartsAt.isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Selected booking slot has already started");
        }

        if (block.getItemType() == ScheduleBlockType.APPOINTMENT_SLOT
                && bookingRepository.countByAvailabilityBlockIdAndStatusNot(block.getId(), BookingStatus.CANCELLED) >= block.getCapacity()) {
            auditLogger.bookingConflict(block.getId(), requiredSpecialistId == null ? client.getId() : requiredSpecialistId);
            throw new BadRequestException("Selected booking slot is already booked");
        }

        if (bookingRepository.existsActiveOverlappingBooking(block.getId(), bookingStartsAt, bookingEndsAt)) {
            auditLogger.bookingConflict(block.getId(), requiredSpecialistId == null ? client.getId() : requiredSpecialistId);
            throw new BadRequestException("Selected booking slot is already booked");
        }

        if (fixedEventRepository.overlapsActiveForSpecialist(block.getSpecialist().getId(), null, bookingStartsAt, bookingEndsAt)) {
            auditLogger.bookingConflict(block.getId(), requiredSpecialistId == null ? client.getId() : requiredSpecialistId);
            throw new BadRequestException("Selected booking slot overlaps a scheduled event");
        }

        Long officeId = block.getOffice() == null ? null : block.getOffice().getId();
        if (availabilityBlockRepository.overlapsBlockedForAvailability(
                block.getSpecialist().getId(),
                officeId,
                bookingStartsAt,
                bookingEndsAt
        )) {
            auditLogger.bookingConflict(block.getId(), requiredSpecialistId == null ? client.getId() : requiredSpecialistId);
            throw new BadRequestException("Selected booking slot is blocked");
        }

        Booking booking = new Booking();
        booking.setUser(client);
        booking.setSpecialist(block.getSpecialist());
        booking.setService(service);
        booking.setOffice(block.getOffice());
        booking.setAvailabilityBlock(block);
        booking.setStatus(BookingStatus.AWAITING_PAYMENT_CONFIRMATION);
        booking.setStartsAt(bookingStartsAt);
        booking.setEndsAt(bookingEndsAt);
        booking.setBookedPrice(service.getBasePrice());
        booking.setReminderOptIn(reminderOptIn);

        Booking savedBooking = bookingRepository.save(booking);
        auditLogger.bookingCreated(savedBooking.getId(), requiredSpecialistId == null ? client.getId() : requiredSpecialistId);
        return savedBooking;
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listUserBookings(long userId, Pageable pageable) {
        return bookingRepository.findUserBookings(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public BookingResponse cancel(long userId, long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new NotFoundException("Booking not found");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }

        if (!booking.getStartsAt().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Booking has already started");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        auditLogger.bookingCancelled(booking.getId(), userId);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<FinanceBookingResponse> listFinanceBookings(
            BookingStatus status,
            Long officeId,
            OffsetDateTime from,
            OffsetDateTime to,
        Pageable pageable
    ) {
        validateOptionalRange(from, to);
        return bookingRepository.findAll(financeFilter(status, officeId, from, to), pageable)
                .map(this::toFinanceResponse);
    }

    @Transactional(readOnly = true)
    public List<SpecialistBookingResponse> listSpecialistBookings(
            long actorId,
            OffsetDateTime from,
            OffsetDateTime to,
            Long requestedSpecialistId
    ) {
        validateSpecialistRange(from, to);
        long specialistId = resolveManagedSpecialistId(actorId, requestedSpecialistId);
        return bookingRepository.findSpecialistBookings(specialistId, from, to)
                .stream()
                .map(this::toSpecialistResponse)
                .toList();
    }

    @Transactional
    public FinanceBookingResponse confirmPayment(long bookingId, long actorId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.AWAITING_PAYMENT_CONFIRMATION) {
            throw new BadRequestException("Only pending payment bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSpecialistPayoutStatus(SpecialistPayoutStatus.PENDING);
        auditLogger.bookingPaymentConfirmed(booking.getId(), actorId);
        return toFinanceResponse(booking);
    }

    @Transactional
    public FinanceBookingResponse markSpecialistPayoutPaid(long bookingId, long actorId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed bookings can be paid out");
        }

        if (booking.getSpecialistPayoutStatus() == SpecialistPayoutStatus.PAID) {
            throw new BadRequestException("Specialist payout is already paid");
        }

        booking.setSpecialistPayoutStatus(SpecialistPayoutStatus.PAID);
        booking.setSpecialistPayoutPaidAt(OffsetDateTime.now());
        booking.setSpecialistPayoutPaidBy(actor);
        auditLogger.specialistPayoutMarkedPaid(booking.getId(), actorId);
        return toFinanceResponse(booking);
    }

    private BookingResponse toResponse(Booking booking) {
        Office office = booking.getOffice();
        ServiceOffering service = booking.getService();
        User specialist = booking.getSpecialist();

        return new BookingResponse(
                booking.getId(),
                booking.getStatus(),
                service.getId(),
                service.getTitleUa(),
                specialist.getId(),
                displayName(specialist),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                booking.getStartsAt(),
                booking.getEndsAt(),
                booking.isReminderOptIn(),
                service.getExternalPaymentUrl()
        );
    }

    private FinanceBookingResponse toFinanceResponse(Booking booking) {
        Office office = booking.getOffice();
        ServiceOffering service = booking.getService();
        User client = booking.getUser();
        User specialist = booking.getSpecialist();
        BigDecimal specialistSharePercent = specialistFinanceSettingsRepository.findById(specialist.getId())
                .map(settings -> settings.getSpecialistSharePercent())
                .orElse(BigDecimal.ZERO);
        BigDecimal specialistShare = FinanceShareCalculator.specialistShare(booking.getBookedPrice(), specialistSharePercent);
        BigDecimal businessShare = FinanceShareCalculator.businessShare(booking.getBookedPrice(), specialistShare);

        return new FinanceBookingResponse(
                booking.getId(),
                booking.getStatus(),
                client.getId(),
                displayName(client),
                client.getPhone() != null ? client.getPhone() : client.getEmail(),
                service.getId(),
                service.getTitleUa(),
                service.getExternalPaymentUrl(),
                booking.getBookedPrice(),
                specialistSharePercent,
                specialistShare,
                businessShare,
                booking.getSpecialistPayoutStatus(),
                booking.getSpecialistPayoutPaidAt(),
                booking.getSpecialistPayoutPaidBy() == null ? null : booking.getSpecialistPayoutPaidBy().getId(),
                specialist.getId(),
                displayName(specialist),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                booking.getStartsAt(),
                booking.getEndsAt(),
                booking.isReminderOptIn(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    private SpecialistBookingResponse toSpecialistResponse(Booking booking) {
        Office office = booking.getOffice();
        ServiceOffering service = booking.getService();
        User client = booking.getUser();

        return new SpecialistBookingResponse(
                booking.getId(),
                booking.getStatus(),
                client.getId(),
                displayName(client),
                client.getPhone() != null ? client.getPhone() : client.getEmail(),
                booking.getSpecialist().getId(),
                displayName(booking.getSpecialist()),
                service.getId(),
                service.getTitleUa(),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                booking.getStartsAt(),
                booking.getEndsAt(),
                booking.isReminderOptIn()
        );
    }

    private long resolveManagedSpecialistId(long actorId, Long requestedSpecialistId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("Specialist not found"));
        if (!actor.getRoles().contains(UserRole.SPECIALIST)) {
            throw new AccessDeniedException("Specialist role is required");
        }
        if (requestedSpecialistId == null || requestedSpecialistId.equals(actorId)) {
            return requestedSpecialistId == null ? actorId : requestedSpecialistId;
        }
        if (!actor.getRoles().contains(UserRole.MASTER)) {
            throw new AccessDeniedException("MASTER role is required to manage another specialist schedule");
        }
        User specialist = userRepository.findById(requestedSpecialistId)
                .orElseThrow(() -> new NotFoundException("Specialist not found"));
        if (!specialist.getRoles().contains(UserRole.SPECIALIST)) {
            throw new AccessDeniedException("Specialist role is required");
        }
        return requestedSpecialistId;
    }

    private void validateSpecialistRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new BadRequestException("Booking range is invalid");
        }

        if (ChronoUnit.DAYS.between(from, to) > MAX_SPECIALIST_QUERY_DAYS) {
            throw new BadRequestException("Booking range is too large");
        }
    }

    private void validateOptionalRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("Booking range is invalid");
        }
    }

    private User findActiveClient(String identifier) {
        String value = identifier.trim();
        User client;

        if (value.contains("@")) {
            client = userRepository.findByEmail(value.toLowerCase(Locale.ROOT)).orElse(null);
        } else {
            client = userRepository.findByPhone(normalizePhone(value)).orElse(null);
        }

        if (client == null || !client.isEnabled()) {
            throw new NotFoundException("Active client account not found");
        }
        return client;
    }

    private String normalizePhone(String phone) {
        String cleaned = phone
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (cleaned.matches("^0\\d{9}$")) {
            cleaned = "+380" + cleaned.substring(1);
        }
        if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }
        if (!cleaned.matches("^\\+[1-9]\\d{9,14}$")) {
            throw new BadRequestException("Invalid client identifier");
        }
        return cleaned;
    }

    private String displayName(User user) {
        String firstName = normalizeNamePart(user.getFirstName());
        String lastName = normalizeNamePart(user.getLastName());

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        if (firstName != null) {
            return firstName;
        }

        if (lastName != null) {
            return lastName;
        }

        return "Specialist";
    }

    private String normalizeNamePart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
