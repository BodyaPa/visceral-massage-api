package com.example.visceralmassageapi.site.service;

import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.site.domain.SiteSettings;
import com.example.visceralmassageapi.site.dto.SiteSettingsRequest;
import com.example.visceralmassageapi.site.dto.SiteSettingsResponse;
import com.example.visceralmassageapi.site.repository.SiteSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SiteSettingsService {

    private static final String DEFAULT_FOOTER_UA = "Вісцеральний масаж, події, новини та особистий запис у просторі Ataraksia.";
    private static final String DEFAULT_FOOTER_EN = "Visceral massage, events, news, and personal booking in the Ataraksia space.";

    private final SiteSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public SiteSettingsResponse get() {
        return settingsRepository.findById(SiteSettings.SINGLETON_ID)
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    @Transactional
    public SiteSettingsResponse update(long actorId, SiteSettingsRequest request) {
        var actor = userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        SiteSettings settings = settingsRepository.findById(SiteSettings.SINGLETON_ID)
                .orElseGet(() -> {
                    SiteSettings created = new SiteSettings();
                    created.setId(SiteSettings.SINGLETON_ID);
                    return created;
                });

        settings.setFooterBodyUa(normalize(request.footerBodyUa()));
        settings.setFooterBodyEn(normalize(request.footerBodyEn()));
        settings.setHomeIntroUa(normalize(request.homeIntroUa()));
        settings.setHomeIntroEn(normalize(request.homeIntroEn()));
        settings.setAboutBodyUa(normalize(request.aboutBodyUa()));
        settings.setAboutBodyEn(normalize(request.aboutBodyEn()));
        settings.setContactBodyUa(normalize(request.contactBodyUa()));
        settings.setContactBodyEn(normalize(request.contactBodyEn()));
        settings.setUpdatedBy(actor);

        SiteSettings saved = settingsRepository.save(settings);
        auditLogger.siteSettingsUpdated(actorId);
        return toResponse(saved);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private SiteSettingsResponse defaultResponse() {
        return new SiteSettingsResponse(
                DEFAULT_FOOTER_UA,
                DEFAULT_FOOTER_EN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private SiteSettingsResponse toResponse(SiteSettings settings) {
        var updatedBy = settings.getUpdatedBy();
        return new SiteSettingsResponse(
                fallback(settings.getFooterBodyUa(), DEFAULT_FOOTER_UA),
                fallback(settings.getFooterBodyEn(), DEFAULT_FOOTER_EN),
                settings.getHomeIntroUa(),
                settings.getHomeIntroEn(),
                settings.getAboutBodyUa(),
                settings.getAboutBodyEn(),
                settings.getContactBodyUa(),
                settings.getContactBodyEn(),
                updatedBy == null ? null : updatedBy.getId(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
