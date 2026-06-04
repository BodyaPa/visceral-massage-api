package com.example.visceralmassageapi.schedule.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
import com.example.visceralmassageapi.schedule.domain.SpecialistAvailabilityBlock;
import com.example.visceralmassageapi.schedule.dto.PublicScheduleAvailabilityResponse;
import com.example.visceralmassageapi.schedule.dto.SpecialistAvailabilityRequest;
import com.example.visceralmassageapi.schedule.dto.SpecialistAvailabilityResponse;
import com.example.visceralmassageapi.schedule.repository.SpecialistAvailabilityBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
            Long specialistId
    ) {
        validateQueryRange(from, to);

        return availabilityBlockRepository.findPublicAvailability(from, to, officeId, specialistId)
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional
    public SpecialistAvailabilityResponse createAvailability(long specialistId, SpecialistAvailabilityRequest request) {
        User specialist = requireSpecialist(specialistId);
        validateBlockRange(request.startsAt(), request.endsAt());

        if (availabilityBlockRepository.overlaps(specialistId, null, request.startsAt(), request.endsAt())) {
            throw new BadRequestException("Schedule block overlaps an existing block");
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
