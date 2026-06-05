package com.example.visceralmassageapi.services.service;

import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.services.dto.AdminServiceResponse;
import com.example.visceralmassageapi.services.dto.PublicServiceResponse;
import com.example.visceralmassageapi.services.dto.ServiceLocale;
import com.example.visceralmassageapi.services.dto.ServiceRequest;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import com.example.visceralmassageapi.services.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;

    @Transactional(readOnly = true)
    public Page<AdminServiceResponse> listAdmin(String query, Boolean active, Pageable pageable) {
        return serviceOfferingRepository.searchAdmin(normalizeQuery(query), active, pageable)
                .map(this::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public AdminServiceResponse getAdmin(long id) {
        return toAdminResponse(requireService(id));
    }

    @Transactional(readOnly = true)
    public Page<PublicServiceResponse> listPublic(ServiceLocale locale, Pageable pageable) {
        return serviceOfferingRepository.findPublic(locale.name(), pageable)
                .map(service -> toPublicResponse(service, locale));
    }

    @Transactional
    public AdminServiceResponse create(ServiceRequest request) {
        ServiceOffering service = new ServiceOffering();
        apply(service, request);
        return toAdminResponse(serviceOfferingRepository.save(service));
    }

    @Transactional
    public AdminServiceResponse update(long id, ServiceRequest request) {
        ServiceOffering service = requireService(id);
        apply(service, request);
        return toAdminResponse(service);
    }

    private ServiceOffering requireService(long id) {
        return serviceOfferingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found"));
    }

    private void apply(ServiceOffering service, ServiceRequest request) {
        service.setTitleUa(normalizeRequired(request.getTitleUa()));
        service.setDescriptionUa(normalizeOptional(request.getDescriptionUa()));
        service.setTitleEn(normalizeOptional(request.getTitleEn()));
        service.setDescriptionEn(normalizeOptional(request.getDescriptionEn()));
        service.setDurationMinutes(request.getDurationMinutes());
        service.setBasePrice(request.getBasePrice());
        service.setBookingMode(request.getBookingMode());
        service.setActive(request.isActive());
        service.setExternalPaymentUrl(normalizeOptional(request.getExternalPaymentUrl()));
    }

    private String normalizeRequired(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private AdminServiceResponse toAdminResponse(ServiceOffering service) {
        return new AdminServiceResponse(
                service.getId(),
                service.getTitleUa(),
                service.getDescriptionUa(),
                service.getTitleEn(),
                service.getDescriptionEn(),
                service.getDurationMinutes(),
                service.getBasePrice(),
                service.getBookingMode(),
                service.isActive(),
                service.getExternalPaymentUrl(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }

    private PublicServiceResponse toPublicResponse(ServiceOffering service, ServiceLocale locale) {
        if (locale == ServiceLocale.EN) {
            return new PublicServiceResponse(
                    service.getId(),
                    service.getTitleEn(),
                    service.getDescriptionEn(),
                    service.getDurationMinutes(),
                    service.getBasePrice(),
                    service.getBookingMode()
            );
        }

        return new PublicServiceResponse(
                service.getId(),
                service.getTitleUa(),
                service.getDescriptionUa(),
                service.getDurationMinutes(),
                service.getBasePrice(),
                service.getBookingMode()
        );
    }
}
