package com.example.visceralmassageapi.finance.service;

import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.finance.domain.FinanceSettings;
import com.example.visceralmassageapi.finance.dto.FinanceSettingsRequest;
import com.example.visceralmassageapi.finance.dto.FinanceSettingsResponse;
import com.example.visceralmassageapi.finance.repository.FinanceSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class FinanceSettingsService {

    private final FinanceSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public FinanceSettingsResponse get() {
        return toResponse(loadSettings());
    }

    @Transactional
    public FinanceSettingsResponse update(long actorId, FinanceSettingsRequest request) {
        var actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        FinanceSettings settings = loadSettings();
        settings.setQuarterlyTaxPercent(request.quarterlyTaxPercent());
        settings.setUpdatedBy(actor);
        FinanceSettings saved = settingsRepository.save(settings);
        auditLogger.financeSettingsUpdated(actorId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BigDecimal quarterlyTaxPercent() {
        return loadSettings().getQuarterlyTaxPercent();
    }

    private FinanceSettings loadSettings() {
        return settingsRepository.findById(FinanceSettings.SINGLETON_ID)
                .orElseGet(() -> {
                    FinanceSettings settings = new FinanceSettings();
                    settings.setId(FinanceSettings.SINGLETON_ID);
                    return settingsRepository.save(settings);
                });
    }

    private FinanceSettingsResponse toResponse(FinanceSettings settings) {
        var updatedBy = settings.getUpdatedBy();
        return new FinanceSettingsResponse(
                settings.getQuarterlyTaxPercent(),
                updatedBy == null ? null : updatedBy.getId(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }
}
