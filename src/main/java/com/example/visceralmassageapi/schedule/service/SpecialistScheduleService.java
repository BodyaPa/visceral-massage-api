package com.example.visceralmassageapi.schedule.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
import com.example.visceralmassageapi.schedule.domain.FixedEvent;
import com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus;
import com.example.visceralmassageapi.schedule.domain.SpecialistAvailabilityBlock;
import com.example.visceralmassageapi.schedule.dto.PublicScheduleAvailabilityResponse;
import com.example.visceralmassageapi.schedule.dto.PublicScheduleUnavailableResponse;
import com.example.visceralmassageapi.schedule.dto.SpecialistAvailabilityRequest;
import com.example.visceralmassageapi.schedule.dto.SpecialistAvailabilityResponse;
import com.example.visceralmassageapi.schedule.repository.FixedEventRepository;
import com.example.visceralmassageapi.schedule.repository.SpecialistAvailabilityBlockRepository;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import com.example.visceralmassageapi.services.entity.ServiceBookingMode;
import com.example.visceralmassageapi.services.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SpecialistScheduleService {

    private static final long MAX_QUERY_DAYS = 93;

    private final SpecialistAvailabilityBlockRepository availabilityBlockRepository;
    private final UserRepository userRepository;
    private final OfficeRepository officeRepository;
    private final BookingRepository bookingRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final FixedEventRepository fixedEventRepository;

    @Transactional(readOnly = true)
    public List<SpecialistAvailabilityResponse> listAvailability(long specialistId, OffsetDateTime from, OffsetDateTime to) {
        validateQueryRange(from, to);

        Set<Long> bookedBlockIds = Set.copyOf(bookingRepository.findBookedAvailabilityBlockIds(specialistId, from, to));

        return availabilityBlockRepository.findOwnRange(specialistId, from, to)
                .stream()
                .map(block -> toResponse(block, bookedBlockIds.contains(block.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicScheduleAvailabilityResponse> listPublicAvailability(
            OffsetDateTime from,
            OffsetDateTime to,
            Long officeId,
            Long specialistId,
            Long serviceId
    ) {
        validateQueryRange(from, to);

        if (serviceId != null) {
            ServiceOffering service = serviceOfferingRepository.findById(serviceId)
                    .orElseThrow(() -> new NotFoundException("Service not found"));

            if (!service.isActive()) {
                throw new BadRequestException("Service is inactive");
            }

            if (service.getBookingMode() != ServiceBookingMode.INDIVIDUAL_APPOINTMENT) {
                throw new BadRequestException("Service does not use individual appointment slots");
            }

            return availabilityBlockRepository.findPublicAvailabilityBlocks(from, to, officeId, specialistId)
                    .stream()
                    .flatMap(block -> toPublicServiceSlots(block, service, from, to).stream())
                    .toList();
        }

        return availabilityBlockRepository.findPublicAvailabilityBlocks(from, to, officeId, specialistId)
                .stream()
                .flatMap(block -> toPublicOpenRanges(block, from, to).stream())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicScheduleUnavailableResponse> listPublicUnavailable(
            OffsetDateTime from,
            OffsetDateTime to,
            Long officeId,
            Long specialistId
    ) {
        validateQueryRange(from, to);
        List<PublicScheduleUnavailableResponse> blocked = availabilityBlockRepository
                .findPublicBlockedRange(from, to, officeId, specialistId)
                .stream()
                .map(this::toBlockedPublicResponse)
                .toList();
        List<PublicScheduleUnavailableResponse> occupied = bookingRepository
                .findPublicOccupiedBookings(from, to, officeId, specialistId)
                .stream()
                .map(this::toOccupiedPublicResponse)
                .toList();
        List<PublicScheduleUnavailableResponse> eventUnavailable = fixedEventRepository
                .findPublicRange(from, to, officeId, specialistId, null)
                .stream()
                .map(this::toFixedEventUnavailableResponse)
                .toList();

        return java.util.stream.Stream.of(blocked.stream(), occupied.stream(), eventUnavailable.stream())
                .flatMap(stream -> stream)
                .sorted(java.util.Comparator.comparing(PublicScheduleUnavailableResponse::startsAt))
                .toList();
    }

    @Transactional
    public SpecialistAvailabilityResponse createAvailability(long specialistId, SpecialistAvailabilityRequest request) {
        User specialist = requireSpecialist(specialistId);
        validateBlockRange(request.startsAt(), request.endsAt());

        if (availabilityBlockRepository.overlapsStatus(specialistId, request.status(), null, request.startsAt(), request.endsAt())) {
            throw new BadRequestException("Schedule block overlaps an existing block");
        }

        if (request.status() == ScheduleBlockStatus.BLOCKED) {
            validateBlockedRangeDoesNotCoverCommitments(specialistId, request.startsAt(), request.endsAt());
        }

        SpecialistAvailabilityBlock block = new SpecialistAvailabilityBlock();
        block.setSpecialist(specialist);
        block.setOffice(resolveOffice(request.officeId()));
        block.setStatus(request.status());
        block.setStartsAt(request.startsAt());
        block.setEndsAt(request.endsAt());
        block.setNotes(normalizeNotes(request.notes()));

        return toResponse(availabilityBlockRepository.save(block), false);
    }

    @Transactional
    public SpecialistAvailabilityResponse updateAvailability(long specialistId, long blockId, SpecialistAvailabilityRequest request) {
        requireSpecialist(specialistId);
        validateBlockRange(request.startsAt(), request.endsAt());

        SpecialistAvailabilityBlock block = availabilityBlockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Schedule block not found"));

        if (!block.getSpecialist().getId().equals(specialistId)) {
            throw new NotFoundException("Schedule block not found");
        }

        boolean hasBookingHistory = bookingRepository.existsByAvailabilityBlockId(blockId);
        boolean changesBookableRange = block.getStatus() != request.status()
                || !block.getStartsAt().isEqual(request.startsAt())
                || !block.getEndsAt().isEqual(request.endsAt())
                || !sameOffice(block.getOffice(), request.officeId());

        if (hasBookingHistory && changesBookableRange) {
            throw new BadRequestException("Schedule block with booking history cannot change time, status or office");
        }

        if (availabilityBlockRepository.overlapsStatus(specialistId, request.status(), blockId, request.startsAt(), request.endsAt())) {
            throw new BadRequestException("Schedule block overlaps an existing block");
        }

        if (request.status() == ScheduleBlockStatus.BLOCKED) {
            validateBlockedRangeDoesNotCoverCommitments(specialistId, request.startsAt(), request.endsAt());
        }

        block.setOffice(resolveOffice(request.officeId()));
        block.setStatus(request.status());
        block.setStartsAt(request.startsAt());
        block.setEndsAt(request.endsAt());
        block.setNotes(normalizeNotes(request.notes()));

        return toResponse(availabilityBlockRepository.save(block), hasBookingHistory);
    }

    private void validateBlockedRangeDoesNotCoverCommitments(
            long specialistId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {
        if (bookingRepository.existsActiveOverlappingSpecialistBooking(specialistId, startsAt, endsAt)) {
            throw new BadRequestException("Blocked time overlaps an existing booking");
        }

        if (fixedEventRepository.overlapsActiveForSpecialist(specialistId, null, startsAt, endsAt)) {
            throw new BadRequestException("Blocked time overlaps an active event");
        }
    }

    @Transactional
    public void deleteAvailability(long specialistId, long blockId) {
        SpecialistAvailabilityBlock block = availabilityBlockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Schedule block not found"));

        if (!block.getSpecialist().getId().equals(specialistId)) {
            throw new NotFoundException("Schedule block not found");
        }

        if (bookingRepository.existsByAvailabilityBlockId(blockId)) {
            throw new BadRequestException("Schedule block with booking history cannot be deleted");
        }

        availabilityBlockRepository.delete(block);
    }

    private User requireSpecialist(long specialistId) {
        User user = userRepository.findById(specialistId)
                .orElseThrow(() -> new NotFoundException("Specialist not found"));

        if (!user.getRoles().contains(UserRole.SPECIALIST)) {
            throw new AccessDeniedException("Specialist role is required");
        }

        return user;
    }

    private Office resolveOffice(Long officeId) {
        if (officeId == null) {
            return null;
        }

        Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new NotFoundException("Office not found"));

        if (!office.isActive()) {
            throw new BadRequestException("Office is inactive");
        }

        return office;
    }

    private void validateQueryRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null) {
            throw new BadRequestException("Schedule range is required");
        }

        validateBlockRange(from, to);

        if (ChronoUnit.DAYS.between(from, to) > MAX_QUERY_DAYS) {
            throw new BadRequestException("Schedule range is too large");
        }
    }

    private void validateBlockRange(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new BadRequestException("Schedule end time must be after start time");
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }

        return notes.trim().replaceAll("\\s+", " ");
    }

    private boolean sameOffice(Office office, Long officeId) {
        Long currentOfficeId = office == null ? null : office.getId();
        return currentOfficeId == null ? officeId == null : currentOfficeId.equals(officeId);
    }

    private SpecialistAvailabilityResponse toResponse(SpecialistAvailabilityBlock block, boolean booked) {
        Office office = block.getOffice();

        return new SpecialistAvailabilityResponse(
                block.getId(),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                block.getStatus(),
                booked,
                block.getStartsAt(),
                block.getEndsAt(),
                block.getNotes(),
                block.getCreatedAt(),
                block.getUpdatedAt()
        );
    }

    private PublicScheduleAvailabilityResponse toPublicResponse(SpecialistAvailabilityBlock block) {
        Office office = block.getOffice();
        User specialist = block.getSpecialist();
        String specialistName = specialistDisplayName(specialist);

        return new PublicScheduleAvailabilityResponse(
                block.getId(),
                specialist.getId(),
                specialistName,
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                block.getStartsAt(),
                block.getEndsAt()
        );
    }

    private PublicScheduleUnavailableResponse toBlockedPublicResponse(SpecialistAvailabilityBlock block) {
        Office office = block.getOffice();
        User specialist = block.getSpecialist();

        return new PublicScheduleUnavailableResponse(
                "blocked-" + block.getId(),
                "UNAVAILABLE",
                specialist.getId(),
                specialistDisplayName(specialist),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                block.getStartsAt(),
                block.getEndsAt()
        );
    }

    private PublicScheduleUnavailableResponse toOccupiedPublicResponse(Booking booking) {
        Office office = booking.getOffice();
        User specialist = booking.getSpecialist();

        return new PublicScheduleUnavailableResponse(
                "occupied-" + booking.getId(),
                "OCCUPIED",
                specialist.getId(),
                specialistDisplayName(specialist),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                booking.getStartsAt(),
                booking.getEndsAt()
        );
    }

    private PublicScheduleUnavailableResponse toFixedEventUnavailableResponse(FixedEvent event) {
        Office office = event.getOffice();
        User specialist = event.getSpecialist();

        return new PublicScheduleUnavailableResponse(
                "event-" + event.getId(),
                "UNAVAILABLE",
                specialist.getId(),
                specialistDisplayName(specialist),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                event.getStartsAt(),
                event.getEndsAt()
        );
    }

    private List<PublicScheduleAvailabilityResponse> toPublicServiceSlots(
            SpecialistAvailabilityBlock block,
            ServiceOffering service,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        int durationMinutes = service.getDurationMinutes();
        OffsetDateTime blockStartsAt = max(block.getStartsAt(), from);
        OffsetDateTime blockEndsAt = min(block.getEndsAt(), to);
        List<OffsetDateTime[]> bookedRanges = bookingRepository
                .findActiveBookingsForAvailabilityBlock(block.getId(), blockStartsAt, blockEndsAt)
                .stream()
                .map(booking -> new OffsetDateTime[]{booking.getStartsAt(), booking.getEndsAt()})
                .toList();
        List<OffsetDateTime[]> occupiedRanges = new ArrayList<>(bookedRanges);
        occupiedRanges.addAll(blockedRanges(block, blockStartsAt, blockEndsAt));
        occupiedRanges.addAll(fixedEventRanges(block, blockStartsAt, blockEndsAt));
        List<PublicScheduleAvailabilityResponse> slots = new ArrayList<>();

        for (OffsetDateTime slotStartsAt = blockStartsAt;
             !slotStartsAt.plusMinutes(durationMinutes).isAfter(blockEndsAt);
             slotStartsAt = slotStartsAt.plusMinutes(durationMinutes)) {
            OffsetDateTime slotEndsAt = slotStartsAt.plusMinutes(durationMinutes);

            if (slotStartsAt.isAfter(OffsetDateTime.now()) && !overlapsAny(slotStartsAt, slotEndsAt, occupiedRanges)) {
                slots.add(toPublicResponse(block, slotStartsAt, slotEndsAt));
            }
        }

        return slots;
    }

    private List<PublicScheduleAvailabilityResponse> toPublicOpenRanges(
            SpecialistAvailabilityBlock block,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        OffsetDateTime blockStartsAt = max(block.getStartsAt(), from);
        OffsetDateTime blockEndsAt = min(block.getEndsAt(), to);
        List<OffsetDateTime[]> occupiedRanges = new ArrayList<>();
        occupiedRanges.addAll(bookingRepository
                .findActiveBookingsForAvailabilityBlock(block.getId(), blockStartsAt, blockEndsAt)
                .stream()
                .map(booking -> new OffsetDateTime[]{booking.getStartsAt(), booking.getEndsAt()})
                .toList());
        occupiedRanges.addAll(blockedRanges(block, blockStartsAt, blockEndsAt));
        occupiedRanges.addAll(fixedEventRanges(block, blockStartsAt, blockEndsAt));
        occupiedRanges.sort(java.util.Comparator.comparing(range -> range[0]));

        List<PublicScheduleAvailabilityResponse> ranges = new ArrayList<>();
        OffsetDateTime cursor = blockStartsAt;
        OffsetDateTime now = OffsetDateTime.now();

        for (OffsetDateTime[] occupiedRange : occupiedRanges) {
            OffsetDateTime occupiedStartsAt = max(occupiedRange[0], blockStartsAt);
            OffsetDateTime occupiedEndsAt = min(occupiedRange[1], blockEndsAt);

            if (cursor.isBefore(occupiedStartsAt) && occupiedStartsAt.isAfter(now)) {
                ranges.add(toPublicResponse(block, max(cursor, now), occupiedStartsAt));
            }

            if (cursor.isBefore(occupiedEndsAt)) {
                cursor = occupiedEndsAt;
            }
        }

        if (cursor.isBefore(blockEndsAt) && blockEndsAt.isAfter(now)) {
            ranges.add(toPublicResponse(block, max(cursor, now), blockEndsAt));
        }

        return ranges.stream()
                .filter(range -> range.endsAt().isAfter(range.startsAt()))
                .toList();
    }

    private List<OffsetDateTime[]> fixedEventRanges(
            SpecialistAvailabilityBlock block,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {
        return fixedEventRepository
                .findActiveOverlappingForSpecialist(block.getSpecialist().getId(), null, startsAt, endsAt)
                .stream()
                .map(event -> new OffsetDateTime[]{event.getStartsAt(), event.getEndsAt()})
                .toList();
    }

    private List<OffsetDateTime[]> blockedRanges(
            SpecialistAvailabilityBlock block,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {
        Office office = block.getOffice();
        return availabilityBlockRepository
                .findBlockedForAvailability(
                        block.getSpecialist().getId(),
                        office == null ? null : office.getId(),
                        startsAt,
                        endsAt
                )
                .stream()
                .map(blocked -> new OffsetDateTime[]{blocked.getStartsAt(), blocked.getEndsAt()})
                .toList();
    }

    private PublicScheduleAvailabilityResponse toPublicResponse(
            SpecialistAvailabilityBlock block,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {
        Office office = block.getOffice();
        User specialist = block.getSpecialist();

        return new PublicScheduleAvailabilityResponse(
                block.getId(),
                specialist.getId(),
                specialistDisplayName(specialist),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                startsAt,
                endsAt
        );
    }

    private boolean overlapsAny(OffsetDateTime startsAt, OffsetDateTime endsAt, List<OffsetDateTime[]> ranges) {
        return ranges.stream().anyMatch(range -> startsAt.isBefore(range[1]) && endsAt.isAfter(range[0]));
    }

    private OffsetDateTime max(OffsetDateTime first, OffsetDateTime second) {
        return first.isAfter(second) ? first : second;
    }

    private OffsetDateTime min(OffsetDateTime first, OffsetDateTime second) {
        return first.isBefore(second) ? first : second;
    }

    private String normalizeNamePart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String specialistDisplayName(User specialist) {
        String firstName = normalizeNamePart(specialist.getFirstName());
        String lastName = normalizeNamePart(specialist.getLastName());

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
}
