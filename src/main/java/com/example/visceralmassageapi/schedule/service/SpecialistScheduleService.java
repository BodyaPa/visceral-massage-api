package com.example.visceralmassageapi.schedule.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.common.config.ScheduleProps;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
import com.example.visceralmassageapi.schedule.domain.FixedEvent;
import com.example.visceralmassageapi.schedule.domain.ScheduleBlockType;
import com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus;
import com.example.visceralmassageapi.schedule.domain.SpecialistAvailabilityBlock;
import com.example.visceralmassageapi.schedule.dto.PublicScheduleAvailabilityResponse;
import com.example.visceralmassageapi.schedule.dto.PublicScheduleUnavailableResponse;
import com.example.visceralmassageapi.schedule.dto.DayPlanCopyRequest;
import com.example.visceralmassageapi.schedule.dto.DayPlanCopyResponse;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    private final ScheduleProps scheduleProps;

    @Transactional(readOnly = true)
    public List<SpecialistAvailabilityResponse> listAvailability(long specialistId, OffsetDateTime from, OffsetDateTime to) {
        return listAvailability(specialistId, from, to, null);
    }

    @Transactional(readOnly = true)
    public List<SpecialistAvailabilityResponse> listAvailability(long actorId, OffsetDateTime from, OffsetDateTime to, Long requestedSpecialistId) {
        return listAvailability(actorId, from, to, requestedSpecialistId, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<SpecialistAvailabilityResponse> listAvailability(
            long actorId,
            OffsetDateTime from,
            OffsetDateTime to,
            Long requestedSpecialistId,
            ScheduleBlockStatus status,
            Long officeId,
            Long serviceId
    ) {
        validateQueryRange(from, to);
        Long managedSpecialistId = resolveManagedSpecialistIdForListing(actorId, requestedSpecialistId);

        Set<Long> bookedBlockIds = managedSpecialistId == null
                ? Set.copyOf(bookingRepository.findBookedAvailabilityBlockIds(from, to))
                : Set.copyOf(bookingRepository.findBookedAvailabilityBlockIds(managedSpecialistId, from, to));

        return availabilityBlockRepository.findManagedRange(managedSpecialistId, from, to, status, officeId, serviceId)
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
                    .filter(block -> isCompatibleWithService(block, service))
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
        List<PublicScheduleUnavailableResponse> bookingBuffers = bookingRepository
                .findPublicOccupiedBookings(
                        from.minusMinutes(appointmentBufferMinutes()),
                        to.plusMinutes(appointmentBufferMinutes()),
                        officeId,
                        specialistId
                )
                .stream()
                .flatMap(booking -> bookingBufferPublicResponses(booking, from, to).stream())
                .toList();
        List<PublicScheduleUnavailableResponse> eventUnavailable = fixedEventRepository
                .findPublicRange(from, to, officeId, specialistId, null)
                .stream()
                .map(this::toFixedEventUnavailableResponse)
                .toList();
        List<PublicScheduleUnavailableResponse> eventBuffers = fixedEventRepository
                .findPublicRange(
                        from.minusMinutes(appointmentBufferMinutes()),
                        to.plusMinutes(appointmentBufferMinutes()),
                        officeId,
                        specialistId,
                        null
                )
                .stream()
                .flatMap(event -> fixedEventBufferPublicResponses(event, from, to).stream())
                .toList();

        return java.util.stream.Stream.of(
                        blocked.stream(),
                        occupied.stream(),
                        bookingBuffers.stream(),
                        eventUnavailable.stream(),
                        eventBuffers.stream()
                )
                .flatMap(stream -> stream)
                .sorted(java.util.Comparator.comparing(PublicScheduleUnavailableResponse::startsAt))
                .toList();
    }

    @Transactional
    public SpecialistAvailabilityResponse createAvailability(long actorId, SpecialistAvailabilityRequest request) {
        long specialistId = resolveManagedSpecialistId(actorId, request.specialistId());
        User specialist = requireSpecialist(specialistId);
        validateBlockRange(request.startsAt(), request.endsAt());
        ScheduleBlockType itemType = resolveItemType(request);
        ServiceOffering service = resolveSlotService(request, itemType);
        Integer capacity = resolveCapacity(request, itemType);
        validateItemRange(request, itemType, service);

        if (availabilityBlockRepository.overlapsStatus(specialistId, request.status(), null, request.startsAt(), request.endsAt())) {
            throw new BadRequestException("Schedule block overlaps an existing block");
        }

        validateConcreteScheduleItemConflicts(
                specialistId,
                null,
                request.officeId(),
                request.status(),
                itemType,
                request.startsAt(),
                request.endsAt()
        );

        if (request.status() == ScheduleBlockStatus.BLOCKED) {
            validateBlockedRangeDoesNotCoverCommitments(specialistId, request.startsAt(), request.endsAt());
        }

        SpecialistAvailabilityBlock block = new SpecialistAvailabilityBlock();
        block.setSpecialist(specialist);
        block.setOffice(resolveOffice(request.officeId()));
        block.setStatus(request.status());
        block.setItemType(itemType);
        block.setService(service);
        block.setCapacity(capacity);
        block.setStartsAt(request.startsAt());
        block.setEndsAt(request.endsAt());
        block.setNotes(normalizeNotes(request.notes()));

        return toResponse(availabilityBlockRepository.save(block), false);
    }

    @Transactional
    public SpecialistAvailabilityResponse updateAvailability(long actorId, long blockId, SpecialistAvailabilityRequest request) {
        validateBlockRange(request.startsAt(), request.endsAt());
        ScheduleBlockType itemType = resolveItemType(request);
        ServiceOffering service = resolveSlotService(request, itemType);
        Integer capacity = resolveCapacity(request, itemType);
        validateItemRange(request, itemType, service);

        SpecialistAvailabilityBlock block = availabilityBlockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Schedule block not found"));

        ensureCanManageBlock(actorId, block);
        long specialistId = block.getSpecialist().getId();

        boolean hasBookingHistory = bookingRepository.existsByAvailabilityBlockId(blockId);
        boolean changesBookableRange = block.getStatus() != request.status()
                || block.getItemType() != itemType
                || !sameService(block.getService(), service)
                || !sameCapacity(block.getCapacity(), capacity)
                || !block.getStartsAt().isEqual(request.startsAt())
                || !block.getEndsAt().isEqual(request.endsAt())
                || !sameOffice(block.getOffice(), request.officeId());

        if (hasBookingHistory && changesBookableRange) {
            throw new BadRequestException("Schedule block with booking history cannot change time, status or office");
        }

        if (availabilityBlockRepository.overlapsStatus(specialistId, request.status(), blockId, request.startsAt(), request.endsAt())) {
            throw new BadRequestException("Schedule block overlaps an existing block");
        }

        validateConcreteScheduleItemConflicts(
                specialistId,
                blockId,
                request.officeId(),
                request.status(),
                itemType,
                request.startsAt(),
                request.endsAt()
        );

        if (request.status() == ScheduleBlockStatus.BLOCKED) {
            validateBlockedRangeDoesNotCoverCommitments(specialistId, request.startsAt(), request.endsAt());
        }

        block.setOffice(resolveOffice(request.officeId()));
        block.setStatus(request.status());
        block.setItemType(itemType);
        block.setService(service);
        block.setCapacity(capacity);
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

    private void validateConcreteScheduleItemConflicts(
            long specialistId,
            Long excludedBlockId,
            Long officeId,
            ScheduleBlockStatus status,
            ScheduleBlockType itemType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {
        if (itemType == ScheduleBlockType.APPOINTMENT_SLOT
                && availabilityBlockRepository.overlapsBlockedForAvailability(
                        specialistId,
                        officeId,
                        excludedBlockId,
                        startsAt,
                        endsAt
                )) {
            throw new BadRequestException("Appointment slot overlaps blocked time");
        }

        if (status == ScheduleBlockStatus.BLOCKED
                && availabilityBlockRepository.overlapsAppointmentSlotForAvailability(
                        specialistId,
                        officeId,
                        excludedBlockId,
                        startsAt,
                        endsAt
                )) {
            throw new BadRequestException("Blocked time overlaps an appointment slot");
        }
    }

    @Transactional
    public void deleteAvailability(long actorId, long blockId) {
        SpecialistAvailabilityBlock block = availabilityBlockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Schedule block not found"));

        ensureCanManageBlock(actorId, block);

        if (bookingRepository.existsByAvailabilityBlockId(blockId)) {
            throw new BadRequestException("Schedule block with booking history cannot be deleted");
        }

        availabilityBlockRepository.delete(block);
    }

    @Transactional
    public DayPlanCopyResponse copyDayPlan(long actorId, DayPlanCopyRequest request) {
        long specialistId = resolveManagedSpecialistId(actorId, request.specialistId());
        validateCopyRequest(request);
        OffsetDateTime sourceStart = request.sourceDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime sourceEnd = sourceStart.plusDays(1);
        List<LocalDate> targetDates = new ArrayList<>(new LinkedHashSet<>(request.targetDates()));
        boolean includeAvailability = request.includeAvailability();
        boolean includeFixedEvents = request.includeFixedEvents();

        if (!includeAvailability && !includeFixedEvents) {
            includeAvailability = true;
            includeFixedEvents = true;
        }

        List<SpecialistAvailabilityBlock> sourceBlocks = includeAvailability
                ? availabilityBlockRepository.findManagedRange(specialistId, sourceStart, sourceEnd)
                : List.of();
        List<FixedEvent> sourceEvents = includeFixedEvents
                ? fixedEventRepository.findManagedRange(specialistId, sourceStart, sourceEnd)
                : List.of();
        List<DayPlanCopyResponse.Conflict> conflicts = findCopyConflicts(specialistId, targetDates, sourceBlocks, sourceEvents);

        if (!conflicts.isEmpty()) {
            return new DayPlanCopyResponse(specialistId, request.sourceDate(), targetDates, 0, 0, conflicts);
        }

        int copiedBlocks = 0;
        int copiedEvents = 0;
        for (LocalDate targetDate : targetDates) {
            for (SpecialistAvailabilityBlock sourceBlock : sourceBlocks) {
                availabilityBlockRepository.save(copyBlock(sourceBlock, targetDate));
                copiedBlocks++;
            }
            for (FixedEvent sourceEvent : sourceEvents) {
                fixedEventRepository.save(copyEvent(sourceEvent, targetDate));
                copiedEvents++;
            }
        }

        return new DayPlanCopyResponse(specialistId, request.sourceDate(), targetDates, copiedBlocks, copiedEvents, List.of());
    }

    private void validateCopyRequest(DayPlanCopyRequest request) {
        if (request.sourceDate() == null || request.targetDates() == null || request.targetDates().isEmpty()) {
            throw new BadRequestException("Source date and target dates are required");
        }
        if (request.targetDates().size() > 31) {
            throw new BadRequestException("Too many target dates");
        }
        if (request.targetDates().contains(request.sourceDate())) {
            throw new BadRequestException("Target dates must not include the source date");
        }
    }

    private List<DayPlanCopyResponse.Conflict> findCopyConflicts(
            long specialistId,
            List<LocalDate> targetDates,
            List<SpecialistAvailabilityBlock> sourceBlocks,
            List<FixedEvent> sourceEvents
    ) {
        List<DayPlanCopyResponse.Conflict> conflicts = new ArrayList<>();
        for (LocalDate targetDate : targetDates) {
            for (SpecialistAvailabilityBlock sourceBlock : sourceBlocks) {
                OffsetDateTime startsAt = moveToDate(sourceBlock.getStartsAt(), targetDate);
                OffsetDateTime endsAt = startsAt.plus(Duration.between(sourceBlock.getStartsAt(), sourceBlock.getEndsAt()));
                String reason = findScheduleConflictReason(specialistId, startsAt, endsAt);
                if (reason != null) {
                    conflicts.add(new DayPlanCopyResponse.Conflict(targetDate, "availability", startsAt, endsAt, reason));
                }
            }
            for (FixedEvent sourceEvent : sourceEvents) {
                OffsetDateTime startsAt = moveToDate(sourceEvent.getStartsAt(), targetDate);
                OffsetDateTime endsAt = startsAt.plus(Duration.between(sourceEvent.getStartsAt(), sourceEvent.getEndsAt()));
                String reason = findScheduleConflictReason(specialistId, startsAt, endsAt);
                if (reason != null) {
                    conflicts.add(new DayPlanCopyResponse.Conflict(targetDate, "fixed_event", startsAt, endsAt, reason));
                }
            }
        }
        return conflicts;
    }

    private String findScheduleConflictReason(long specialistId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (availabilityBlockRepository.overlaps(specialistId, null, startsAt, endsAt)) {
            return "overlaps existing schedule item";
        }

        OffsetDateTime bufferedStartsAt = startsAt.minusMinutes(appointmentBufferMinutes());
        OffsetDateTime bufferedEndsAt = endsAt.plusMinutes(appointmentBufferMinutes());

        if (bookingRepository.existsActiveOverlappingSpecialistBooking(specialistId, bufferedStartsAt, bufferedEndsAt)) {
            return "is too close to existing booking";
        }
        if (fixedEventRepository.overlapsActiveForSpecialist(specialistId, null, bufferedStartsAt, bufferedEndsAt)) {
            return "is too close to existing event";
        }
        return null;
    }

    private SpecialistAvailabilityBlock copyBlock(SpecialistAvailabilityBlock source, LocalDate targetDate) {
        OffsetDateTime startsAt = moveToDate(source.getStartsAt(), targetDate);
        SpecialistAvailabilityBlock copy = new SpecialistAvailabilityBlock();
        copy.setSpecialist(source.getSpecialist());
        copy.setOffice(source.getOffice());
        copy.setStatus(source.getStatus());
        copy.setItemType(source.getItemType());
        copy.setService(source.getService());
        copy.setCapacity(source.getCapacity());
        copy.setStartsAt(startsAt);
        copy.setEndsAt(startsAt.plus(Duration.between(source.getStartsAt(), source.getEndsAt())));
        copy.setNotes(source.getNotes());
        return copy;
    }

    private FixedEvent copyEvent(FixedEvent source, LocalDate targetDate) {
        OffsetDateTime startsAt = moveToDate(source.getStartsAt(), targetDate);
        FixedEvent copy = new FixedEvent();
        copy.setSpecialist(source.getSpecialist());
        copy.setService(source.getService());
        copy.setOffice(source.getOffice());
        copy.setStartsAt(startsAt);
        copy.setEndsAt(startsAt.plus(Duration.between(source.getStartsAt(), source.getEndsAt())));
        copy.setCapacity(source.getCapacity());
        copy.setNote(source.getNote());
        copy.setActive(source.isActive());
        return copy;
    }

    private OffsetDateTime moveToDate(OffsetDateTime value, LocalDate targetDate) {
        return OffsetDateTime.of(targetDate, value.toLocalTime(), value.getOffset());
    }

    private Long resolveManagedSpecialistId(long actorId, Long requestedSpecialistId) {
        User actor = requireSpecialist(actorId);
        if (requestedSpecialistId == null || requestedSpecialistId.equals(actorId)) {
            return requestedSpecialistId == null ? actorId : requestedSpecialistId;
        }
        if (!actor.getRoles().contains(UserRole.MASTER)) {
            throw new AccessDeniedException("MASTER role is required to manage another specialist schedule");
        }
        requireSpecialist(requestedSpecialistId);
        return requestedSpecialistId;
    }

    private Long resolveManagedSpecialistIdForListing(long actorId, Long requestedSpecialistId) {
        User actor = requireSpecialist(actorId);
        if (requestedSpecialistId == null) {
            return actor.getRoles().contains(UserRole.MASTER) ? null : actorId;
        }
        if (requestedSpecialistId.equals(actorId)) {
            return actorId;
        }
        if (!actor.getRoles().contains(UserRole.MASTER)) {
            throw new AccessDeniedException("MASTER role is required to manage another specialist schedule");
        }
        requireSpecialist(requestedSpecialistId);
        return requestedSpecialistId;
    }

    private void ensureCanManageBlock(long actorId, SpecialistAvailabilityBlock block) {
        User actor = requireSpecialist(actorId);
        if (block.getSpecialist().getId().equals(actorId)) {
            return;
        }
        if (!actor.getRoles().contains(UserRole.MASTER)) {
            throw new AccessDeniedException("MASTER role is required to manage another specialist schedule");
        }
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

    private ScheduleBlockType resolveItemType(SpecialistAvailabilityRequest request) {
        if (request.status() == ScheduleBlockStatus.BLOCKED) {
            return ScheduleBlockType.BLOCK;
        }
        if (request.itemType() != null) {
            if (request.itemType() == ScheduleBlockType.BLOCK) {
                throw new BadRequestException("Blocked item type requires BLOCKED status");
            }
            return request.itemType();
        }
        return request.serviceId() == null ? ScheduleBlockType.OPEN_RANGE : ScheduleBlockType.APPOINTMENT_SLOT;
    }

    private ServiceOffering resolveSlotService(SpecialistAvailabilityRequest request, ScheduleBlockType itemType) {
        if (itemType != ScheduleBlockType.APPOINTMENT_SLOT) {
            if (request.serviceId() != null) {
                throw new BadRequestException("Service is allowed only for appointment slots");
            }
            return null;
        }

        if (request.serviceId() == null) {
            throw new BadRequestException("Appointment slot service is required");
        }

        ServiceOffering service = serviceOfferingRepository.findById(request.serviceId())
                .orElseThrow(() -> new NotFoundException("Service not found"));
        if (!service.isActive()) {
            throw new BadRequestException("Service is inactive");
        }
        if (service.getBookingMode() != ServiceBookingMode.INDIVIDUAL_APPOINTMENT) {
            throw new BadRequestException("Appointment slots require an individual appointment service");
        }
        return service;
    }

    private Integer resolveCapacity(SpecialistAvailabilityRequest request, ScheduleBlockType itemType) {
        if (itemType != ScheduleBlockType.APPOINTMENT_SLOT) {
            if (request.capacity() != null) {
                throw new BadRequestException("Capacity is allowed only for appointment slots");
            }
            return null;
        }
        return request.capacity() == null ? 1 : request.capacity();
    }

    private void validateItemRange(SpecialistAvailabilityRequest request, ScheduleBlockType itemType, ServiceOffering service) {
        if (request.status() == ScheduleBlockStatus.BLOCKED && itemType != ScheduleBlockType.BLOCK) {
            throw new BadRequestException("Blocked schedule items must use BLOCK type");
        }
        if (itemType == ScheduleBlockType.APPOINTMENT_SLOT) {
            OffsetDateTime expectedEndsAt = request.startsAt().plusMinutes(service.getDurationMinutes());
            if (!expectedEndsAt.isEqual(request.endsAt())) {
                throw new BadRequestException("Appointment slot duration must match the selected service");
            }
        }
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

    private boolean sameService(ServiceOffering current, ServiceOffering next) {
        Long currentId = current == null ? null : current.getId();
        Long nextId = next == null ? null : next.getId();
        return currentId == null ? nextId == null : currentId.equals(nextId);
    }

    private boolean sameCapacity(Integer current, Integer next) {
        return current == null ? next == null : current.equals(next);
    }

    private SpecialistAvailabilityResponse toResponse(SpecialistAvailabilityBlock block, boolean booked) {
        Office office = block.getOffice();
        User specialist = block.getSpecialist();
        ServiceOffering service = block.getService();

        return new SpecialistAvailabilityResponse(
                block.getId(),
                specialist.getId(),
                specialistDisplayName(specialist),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                block.getStatus(),
                block.getItemType(),
                service == null ? null : service.getId(),
                service == null ? null : service.getTitleUa(),
                block.getCapacity(),
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
                office == null ? null : office.getAddress(),
                office == null ? null : visibleDirections(office),
                office == null ? null : office.getGoogleMapsUrl(),
                office == null ? null : office.getPhotoMediaId(),
                mediaUrl(office, office == null ? null : office.getPhotoMediaId()),
                office == null ? null : office.getVideoMediaId(),
                mediaUrl(office, office == null ? null : office.getVideoMediaId()),
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

    private List<PublicScheduleUnavailableResponse> bookingBufferPublicResponses(
            Booking booking,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return bufferPublicResponses(
                "booking-buffer-" + booking.getId(),
                booking.getSpecialist(),
                booking.getOffice(),
                booking.getStartsAt(),
                booking.getEndsAt(),
                from,
                to
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

    private List<PublicScheduleUnavailableResponse> fixedEventBufferPublicResponses(
            FixedEvent event,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return bufferPublicResponses(
                "event-buffer-" + event.getId(),
                event.getSpecialist(),
                event.getOffice(),
                event.getStartsAt(),
                event.getEndsAt(),
                from,
                to
        );
    }

    private List<PublicScheduleUnavailableResponse> bufferPublicResponses(
            String idPrefix,
            User specialist,
            Office office,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        List<PublicScheduleUnavailableResponse> buffers = new ArrayList<>(1);
        addBufferPublicResponse(buffers, idPrefix + "-after", specialist, office, endsAt, endsAt.plusMinutes(appointmentBufferMinutes()), from, to);
        return buffers;
    }

    private void addBufferPublicResponse(
            List<PublicScheduleUnavailableResponse> buffers,
            String id,
            User specialist,
            Office office,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        OffsetDateTime clippedStartsAt = max(startsAt, from);
        OffsetDateTime clippedEndsAt = min(endsAt, to);
        if (!clippedEndsAt.isAfter(clippedStartsAt)) {
            return;
        }

        buffers.add(new PublicScheduleUnavailableResponse(
                id,
                "BUFFER",
                specialist.getId(),
                specialistDisplayName(specialist),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                clippedStartsAt,
                clippedEndsAt
        ));
    }

    private List<PublicScheduleAvailabilityResponse> toPublicServiceSlots(
            SpecialistAvailabilityBlock block,
            ServiceOffering service,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        if (block.getItemType() == ScheduleBlockType.APPOINTMENT_SLOT && !isCompatibleWithService(block, service)) {
            return List.of();
        }

        int durationMinutes = service.getDurationMinutes();
        OffsetDateTime blockStartsAt = max(block.getStartsAt(), from);
        OffsetDateTime blockEndsAt = min(block.getEndsAt(), to);
        List<OffsetDateTime[]> commitmentRanges = new ArrayList<>(bookedRanges(block, blockStartsAt, blockEndsAt));
        commitmentRanges.addAll(fixedEventRanges(block, blockStartsAt, blockEndsAt));
        List<OffsetDateTime[]> blockedRanges = blockedRanges(block, blockStartsAt, blockEndsAt);
        List<PublicScheduleAvailabilityResponse> slots = new ArrayList<>();

        for (OffsetDateTime slotStartsAt = blockStartsAt;
             !slotStartsAt.plusMinutes(durationMinutes).isAfter(blockEndsAt);
             slotStartsAt = slotStartsAt.plusMinutes(durationMinutes)) {
            OffsetDateTime slotEndsAt = slotStartsAt.plusMinutes(durationMinutes);

            if (slotStartsAt.isAfter(OffsetDateTime.now())
                    && !overlapsAny(slotStartsAt, slotEndsAt.plusMinutes(appointmentBufferMinutes()), commitmentRanges)
                    && !overlapsAny(slotStartsAt, slotEndsAt, blockedRanges)) {
                slots.add(toPublicResponse(block, slotStartsAt, slotEndsAt));
            }
        }

        return slots;
    }

    private boolean isCompatibleWithService(SpecialistAvailabilityBlock block, ServiceOffering service) {
        ServiceOffering blockService = block.getService();
        return blockService == null || blockService.getId().equals(service.getId());
    }

    private List<PublicScheduleAvailabilityResponse> toPublicOpenRanges(
            SpecialistAvailabilityBlock block,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        OffsetDateTime blockStartsAt = max(block.getStartsAt(), from);
        OffsetDateTime blockEndsAt = min(block.getEndsAt(), to);
        List<OffsetDateTime[]> occupiedRanges = new ArrayList<>(bookedRanges(block, blockStartsAt, blockEndsAt));
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
                .findActiveOverlappingForSpecialist(block.getSpecialist().getId(), null, startsAt.minusMinutes(appointmentBufferMinutes()), endsAt.plusMinutes(appointmentBufferMinutes()))
                .stream()
                .map(event -> bufferedRange(event.getStartsAt(), event.getEndsAt()))
                .toList();
    }

    private List<OffsetDateTime[]> bookedRanges(
            SpecialistAvailabilityBlock block,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt
    ) {
        return bookingRepository
                .findPublicOccupiedBookings(
                        startsAt.minusMinutes(appointmentBufferMinutes()),
                        endsAt.plusMinutes(appointmentBufferMinutes()),
                        null,
                        block.getSpecialist().getId()
                )
                .stream()
                .map(booking -> bufferedRange(booking.getStartsAt(), booking.getEndsAt()))
                .toList();
    }

    private OffsetDateTime[] bufferedRange(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        return new OffsetDateTime[]{
                startsAt,
                endsAt.plusMinutes(appointmentBufferMinutes())
        };
    }

    private int appointmentBufferMinutes() {
        return scheduleProps.getAppointmentBufferMinutes();
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
                office == null ? null : office.getAddress(),
                office == null ? null : visibleDirections(office),
                office == null ? null : office.getGoogleMapsUrl(),
                office == null ? null : office.getPhotoMediaId(),
                mediaUrl(office, office == null ? null : office.getPhotoMediaId()),
                office == null ? null : office.getVideoMediaId(),
                mediaUrl(office, office == null ? null : office.getVideoMediaId()),
                startsAt,
                endsAt
        );
    }

    private String visibleDirections(Office office) {
        return office.getDirections() == null ? office.getLocationDetails() : office.getDirections();
    }

    private String mediaUrl(Office office, java.util.UUID mediaId) {
        if (office == null || mediaId == null) {
            return null;
        }
        return "/api/offices/" + office.getId() + "/media/" + mediaId + "/content";
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
