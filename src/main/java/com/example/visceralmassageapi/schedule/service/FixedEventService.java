package com.example.visceralmassageapi.schedule.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.common.config.ScheduleProps;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
import com.example.visceralmassageapi.schedule.domain.FixedEvent;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollment;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;
import com.example.visceralmassageapi.schedule.dto.FixedEventEnrollmentRequest;
import com.example.visceralmassageapi.schedule.dto.PublicFixedEventResponse;
import com.example.visceralmassageapi.schedule.dto.SpecialistFixedEventEnrollmentResponse;
import com.example.visceralmassageapi.schedule.dto.SpecialistFixedEventRequest;
import com.example.visceralmassageapi.schedule.dto.SpecialistFixedEventResponse;
import com.example.visceralmassageapi.schedule.repository.FixedEventEnrollmentRepository;
import com.example.visceralmassageapi.schedule.repository.FixedEventRepository;
import com.example.visceralmassageapi.schedule.repository.SpecialistAvailabilityBlockRepository;
import com.example.visceralmassageapi.services.dto.ServiceLocale;
import com.example.visceralmassageapi.services.entity.ServiceBookingMode;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import com.example.visceralmassageapi.services.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FixedEventService {

    private static final long MAX_QUERY_DAYS = 93;
    private final FixedEventRepository fixedEventRepository;
    private final FixedEventEnrollmentRepository fixedEventEnrollmentRepository;
    private final UserRepository userRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final OfficeRepository officeRepository;
    private final BookingRepository bookingRepository;
    private final SpecialistAvailabilityBlockRepository availabilityBlockRepository;
    private final ScheduleProps scheduleProps;

    @Transactional(readOnly = true)
    public List<PublicFixedEventResponse> listPublic(
            OffsetDateTime from,
            OffsetDateTime to,
            Long officeId,
            Long specialistId,
            Long serviceId,
            Long currentUserId,
            ServiceLocale locale
    ) {
        validateRange(from, to);
        List<FixedEvent> events = fixedEventRepository.findPublicRange(from, to, officeId, specialistId, serviceId);
        List<Long> eventIds = events.stream().map(FixedEvent::getId).toList();
        Map<Long, Integer> enrollmentCounts = new HashMap<>();
        Set<Long> enrolledEventIds = new HashSet<>();

        if (!eventIds.isEmpty()) {
            for (FixedEventEnrollment enrollment : fixedEventEnrollmentRepository.findActiveForEvents(eventIds)) {
                long eventId = enrollment.getEvent().getId();
                enrollmentCounts.merge(eventId, 1, Integer::sum);
                if (currentUserId != null && enrollment.getUser().getId().equals(currentUserId)) {
                    enrolledEventIds.add(eventId);
                }
            }
        }

        return events.stream()
                .map(event -> toResponse(
                        event,
                        enrollmentCounts.getOrDefault(event.getId(), 0),
                        enrolledEventIds.contains(event.getId()),
                        locale
                ))
                .toList();
    }

    @Transactional
    public PublicFixedEventResponse enroll(long eventId, long userId, FixedEventEnrollmentRequest request, ServiceLocale locale) {
        FixedEvent event = fixedEventRepository.findForEnrollment(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!event.isActive() || !event.getService().isActive()) {
            throw new BadRequestException("Event is not available");
        }

        if (!event.getStartsAt().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Event has already started");
        }

        Optional<FixedEventEnrollment> existingEnrollment = fixedEventEnrollmentRepository
                .findByEventIdAndUserIdAndStatus(eventId, userId, FixedEventEnrollmentStatus.ACTIVE);
        if (existingEnrollment.isPresent()) {
            int enrolledCount = (int) fixedEventEnrollmentRepository.countByEventIdAndStatus(eventId, FixedEventEnrollmentStatus.ACTIVE);
            return toResponse(event, enrolledCount, true, locale);
        }

        int enrolledCount = (int) fixedEventEnrollmentRepository.countByEventIdAndStatus(eventId, FixedEventEnrollmentStatus.ACTIVE);
        if (enrolledCount >= event.getCapacity()) {
            throw new BadRequestException("Event is full");
        }

        FixedEventEnrollment enrollment = new FixedEventEnrollment();
        enrollment.setEvent(event);
        enrollment.setUser(user);
        enrollment.setStatus(FixedEventEnrollmentStatus.ACTIVE);
        enrollment.setReminderOptIn(request.reminderOptIn());
        fixedEventEnrollmentRepository.save(enrollment);

        return toResponse(event, enrolledCount + 1, true, locale);
    }

    @Transactional(readOnly = true)
    public List<PublicFixedEventResponse> listMyEnrollments(long userId, OffsetDateTime from, OffsetDateTime to, ServiceLocale locale) {
        validateRange(from, to);
        List<FixedEventEnrollment> enrollments = fixedEventEnrollmentRepository.findActiveForUser(userId, from, to);
        List<Long> eventIds = enrollments.stream().map(enrollment -> enrollment.getEvent().getId()).toList();
        Map<Long, Integer> enrollmentCounts = new HashMap<>();

        if (!eventIds.isEmpty()) {
            for (FixedEventEnrollment enrollment : fixedEventEnrollmentRepository.findActiveForEvents(eventIds)) {
                enrollmentCounts.merge(enrollment.getEvent().getId(), 1, Integer::sum);
            }
        }

        return enrollments.stream()
                .map(FixedEventEnrollment::getEvent)
                .map(event -> toResponse(event, enrollmentCounts.getOrDefault(event.getId(), 0), true, locale))
                .toList();
    }

    @Transactional
    public PublicFixedEventResponse cancelEnrollment(long eventId, long userId, ServiceLocale locale) {
        FixedEventEnrollment enrollment = fixedEventEnrollmentRepository
                .findByEventIdAndUserIdAndStatus(eventId, userId, FixedEventEnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Event enrollment not found"));
        FixedEvent event = enrollment.getEvent();

        if (!event.getStartsAt().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Event has already started");
        }

        enrollment.setStatus(FixedEventEnrollmentStatus.CANCELLED);
        fixedEventEnrollmentRepository.save(enrollment);
        int enrolledCount = (int) fixedEventEnrollmentRepository.countByEventIdAndStatus(eventId, FixedEventEnrollmentStatus.ACTIVE);

        return toResponse(event, Math.max(enrolledCount, 0), false, locale);
    }

    @Transactional(readOnly = true)
    public List<SpecialistFixedEventResponse> listOwn(long specialistId, OffsetDateTime from, OffsetDateTime to) {
        return listOwn(specialistId, from, to, null);
    }

    @Transactional(readOnly = true)
    public List<SpecialistFixedEventResponse> listOwn(long actorId, OffsetDateTime from, OffsetDateTime to, Long requestedSpecialistId) {
        validateRange(from, to);
        long specialistId = resolveManagedSpecialistId(actorId, requestedSpecialistId);
        return fixedEventRepository.findManagedRange(specialistId, from, to)
                .stream()
                .map(this::toSpecialistResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpecialistFixedEventEnrollmentResponse> listOwnEnrollments(long specialistId, OffsetDateTime from, OffsetDateTime to) {
        return listOwnEnrollments(specialistId, from, to, null);
    }

    @Transactional(readOnly = true)
    public List<SpecialistFixedEventEnrollmentResponse> listOwnEnrollments(long actorId, OffsetDateTime from, OffsetDateTime to, Long requestedSpecialistId) {
        validateRange(from, to);
        long specialistId = resolveManagedSpecialistId(actorId, requestedSpecialistId);
        return fixedEventEnrollmentRepository.findForSpecialistEvents(specialistId, from, to)
                .stream()
                .map(this::toSpecialistEnrollmentResponse)
                .toList();
    }

    @Transactional
    public SpecialistFixedEventResponse createOwn(long actorId, SpecialistFixedEventRequest request) {
        long specialistId = resolveManagedSpecialistId(actorId, request.specialistId());
        User specialist = userRepository.findById(specialistId)
                .orElseThrow(() -> new NotFoundException("Specialist not found"));
        ServiceOffering service = serviceOfferingRepository.findById(request.serviceId())
                .orElseThrow(() -> new NotFoundException("Service not found"));

        validateFixedEventRequest(request, service, true);
        validateScheduleConflicts(specialistId, null, request);

        FixedEvent event = new FixedEvent();
        event.setSpecialist(specialist);
        event.setService(service);
        event.setOffice(resolveOffice(request.officeId()));
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setCapacity(request.capacity());
        event.setNote(normalizeNote(request.note()));
        event.setActive(request.active());

        return toSpecialistResponse(fixedEventRepository.save(event));
    }

    @Transactional
    public SpecialistFixedEventResponse updateOwn(long actorId, long eventId, SpecialistFixedEventRequest request) {
        FixedEvent event = fixedEventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        ensureCanManageEvent(actorId, event);
        long specialistId = event.getSpecialist().getId();

        ServiceOffering service = serviceOfferingRepository.findById(request.serviceId())
                .orElseThrow(() -> new NotFoundException("Service not found"));
        boolean timeChanged = hasEventTimeChanged(event, request);
        validateFixedEventRequest(request, service, timeChanged);
        validateScheduleConflicts(specialistId, eventId, request);

        int enrolledCount = (int) fixedEventEnrollmentRepository.countByEventIdAndStatus(
                eventId,
                FixedEventEnrollmentStatus.ACTIVE
        );
        if (request.capacity() < enrolledCount) {
            throw new BadRequestException("Event capacity cannot be below current enrollments");
        }

        event.setService(service);
        event.setOffice(resolveOffice(request.officeId()));
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setCapacity(request.capacity());
        event.setNote(normalizeNote(request.note()));
        event.setActive(request.active());

        return toSpecialistResponse(event);
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

    private void ensureCanManageEvent(long actorId, FixedEvent event) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("Specialist not found"));
        if (!actor.getRoles().contains(UserRole.SPECIALIST)) {
            throw new AccessDeniedException("Specialist role is required");
        }
        if (event.getSpecialist().getId().equals(actorId)) {
            return;
        }
        if (!actor.getRoles().contains(UserRole.MASTER)) {
            throw new AccessDeniedException("MASTER role is required to manage another specialist schedule");
        }
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new BadRequestException("Event range is invalid");
        }

        if (ChronoUnit.DAYS.between(from, to) > MAX_QUERY_DAYS) {
            throw new BadRequestException("Event range is too large");
        }
    }

    private boolean hasEventTimeChanged(FixedEvent event, SpecialistFixedEventRequest request) {
        return request.startsAt() == null
                || request.endsAt() == null
                || !request.startsAt().isEqual(event.getStartsAt())
                || !request.endsAt().isEqual(event.getEndsAt());
    }

    private void validateFixedEventRequest(SpecialistFixedEventRequest request, ServiceOffering service, boolean requireFutureStart) {
        if (!service.isActive() || service.getBookingMode() != ServiceBookingMode.FIXED_EVENT) {
            throw new BadRequestException("Service must be an active fixed event service");
        }

        if (request.startsAt() == null || request.endsAt() == null || !request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("Event end time must be after start time");
        }

        if (requireFutureStart && !request.startsAt().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Event must start in the future");
        }
    }

    private void validateScheduleConflicts(long specialistId, Long excludedEventId, SpecialistFixedEventRequest request) {
        if (!request.active()) {
            return;
        }

        if (availabilityBlockRepository.overlapsBlocked(specialistId, request.startsAt(), request.endsAt())) {
            throw new BadRequestException("Event overlaps blocked specialist time");
        }

        OffsetDateTime bufferedStartsAt = request.startsAt().minusMinutes(scheduleProps.getAppointmentBufferMinutes());
        OffsetDateTime bufferedEndsAt = request.endsAt().plusMinutes(scheduleProps.getAppointmentBufferMinutes());

        if (bookingRepository.existsActiveOverlappingSpecialistBooking(specialistId, bufferedStartsAt, bufferedEndsAt)) {
            throw new BadRequestException("Event is too close to an existing individual booking");
        }

        if (fixedEventRepository.overlapsActiveForSpecialist(specialistId, excludedEventId, bufferedStartsAt, bufferedEndsAt)) {
            throw new BadRequestException("Event is too close to another active event");
        }
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

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim().replaceAll("\\s+", " ");
    }

    private PublicFixedEventResponse toResponse(FixedEvent event, int enrolledCount, boolean enrolled, ServiceLocale locale) {
        ServiceOffering service = event.getService();
        Office office = event.getOffice();
        User specialist = event.getSpecialist();
        int remainingPlaces = Math.max(event.getCapacity() - enrolledCount, 0);

        return new PublicFixedEventResponse(
                event.getId(),
                service.getId(),
                locale == ServiceLocale.EN && service.getTitleEn() != null ? service.getTitleEn() : service.getTitleUa(),
                locale == ServiceLocale.EN && service.getDescriptionEn() != null ? service.getDescriptionEn() : service.getDescriptionUa(),
                specialist.getId(),
                specialistDisplayName(specialist),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                office == null ? null : office.getAddress(),
                office == null ? null : visibleDirections(office),
                office == null ? null : office.getPhotoUrl(),
                office == null ? null : office.getVideoUrl(),
                office == null ? null : office.getGoogleMapsUrl(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getCapacity(),
                enrolledCount,
                remainingPlaces,
                remainingPlaces <= 0,
                enrolled,
                service.getBasePrice(),
                event.getNote()
        );
    }

    private String visibleDirections(Office office) {
        return office.getDirections() == null ? office.getLocationDetails() : office.getDirections();
    }

    private SpecialistFixedEventResponse toSpecialistResponse(FixedEvent event) {
        ServiceOffering service = event.getService();
        Office office = event.getOffice();
        int enrolledCount = (int) fixedEventEnrollmentRepository.countByEventIdAndStatus(
                event.getId(),
                FixedEventEnrollmentStatus.ACTIVE
        );

        return new SpecialistFixedEventResponse(
                event.getId(),
                event.getSpecialist().getId(),
                specialistDisplayName(event.getSpecialist()),
                service.getId(),
                service.getTitleUa(),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getCapacity(),
                enrolledCount,
                service.getBasePrice(),
                event.getNote(),
                event.isActive(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private SpecialistFixedEventEnrollmentResponse toSpecialistEnrollmentResponse(FixedEventEnrollment enrollment) {
        FixedEvent event = enrollment.getEvent();
        ServiceOffering service = event.getService();
        User client = enrollment.getUser();

        return new SpecialistFixedEventEnrollmentResponse(
                enrollment.getId(),
                event.getId(),
                service.getTitleUa(),
                event.getStartsAt(),
                event.getEndsAt(),
                client.getId(),
                specialistDisplayName(client),
                clientContact(client),
                enrollment.getStatus(),
                enrollment.isReminderOptIn(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }

    private String clientContact(User client) {
        if (client.getPhone() != null && !client.getPhone().isBlank()) {
            return client.getPhone();
        }
        if (client.getEmail() != null && !client.getEmail().isBlank()) {
            return client.getEmail();
        }
        return "";
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

    private String normalizeNamePart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
