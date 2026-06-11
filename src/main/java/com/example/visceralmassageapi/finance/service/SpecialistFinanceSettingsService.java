package com.example.visceralmassageapi.finance.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.finance.domain.SpecialistFinanceSettings;
import com.example.visceralmassageapi.finance.dto.SpecialistFinanceSettingsRequest;
import com.example.visceralmassageapi.finance.dto.SpecialistFinanceSettingsResponse;
import com.example.visceralmassageapi.finance.repository.SpecialistFinanceSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecialistFinanceSettingsService {

    private final SpecialistFinanceSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public SpecialistFinanceSettingsResponse get(long specialistId) {
        User specialist = requireSpecialist(specialistId);
        return settingsRepository.findById(specialistId)
                .map(this::toResponse)
                .orElseGet(() -> defaultResponse(specialist));
    }

    @Transactional(readOnly = true)
    public List<SpecialistFinanceSettingsResponse> list() {
        List<User> specialists = userRepository.findEnabledUsersByRole(UserRole.SPECIALIST);
        if (specialists.isEmpty()) {
            return List.of();
        }

        List<Long> specialistIds = specialists.stream()
                .map(User::getId)
                .toList();
        Map<Long, SpecialistFinanceSettings> settingsBySpecialistId = settingsRepository
                .findBySpecialistUserIdIn(specialistIds)
                .stream()
                .collect(Collectors.toMap(settings -> settings.getSpecialist().getId(), Function.identity()));

        return specialists.stream()
                .map(specialist -> {
                    SpecialistFinanceSettings settings = settingsBySpecialistId.get(specialist.getId());
                    return settings == null ? defaultResponse(specialist) : toResponse(settings);
                })
                .toList();
    }

    @Transactional
    public SpecialistFinanceSettingsResponse update(
            long specialistId,
            long actorId,
            SpecialistFinanceSettingsRequest request
    ) {
        User specialist = requireSpecialist(specialistId);
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        SpecialistFinanceSettings settings = settingsRepository.findById(specialistId)
                .orElseGet(() -> {
                    SpecialistFinanceSettings created = new SpecialistFinanceSettings();
                    created.setSpecialist(specialist);
                    return created;
                });

        settings.setSpecialistSharePercent(request.specialistSharePercent());
        settings.setUpdatedBy(actor);
        SpecialistFinanceSettings saved = settingsRepository.save(settings);
        auditLogger.specialistFinanceSettingsUpdated(specialistId, actorId);
        return toResponse(saved);
    }

    private User requireSpecialist(long specialistId) {
        User specialist = userRepository.findById(specialistId)
                .orElseThrow(() -> new NotFoundException("Specialist not found"));

        if (!specialist.getRoles().contains(UserRole.SPECIALIST)) {
            throw new AccessDeniedException("Specialist role is required");
        }

        return specialist;
    }

    private SpecialistFinanceSettingsResponse defaultResponse(User specialist) {
        return new SpecialistFinanceSettingsResponse(
                specialist.getId(),
                displayName(specialist),
                BigDecimal.ZERO,
                null,
                null,
                null
        );
    }

    private SpecialistFinanceSettingsResponse toResponse(SpecialistFinanceSettings settings) {
        User updatedBy = settings.getUpdatedBy();
        return new SpecialistFinanceSettingsResponse(
                settings.getSpecialist().getId(),
                displayName(settings.getSpecialist()),
                settings.getSpecialistSharePercent(),
                updatedBy == null ? null : updatedBy.getId(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }

    private String displayName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
