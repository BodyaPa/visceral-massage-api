package com.example.visceralmassageapi.services.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.example.visceralmassageapi.services.entity.ServiceBookingMode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class AdminServiceResponse {
    private Long id;
    private String titleUa;
    private String descriptionUa;
    private String titleEn;
    private String descriptionEn;
    private Integer durationMinutes;
    private BigDecimal basePrice;
    private ServiceBookingMode bookingMode;
    private boolean active;
    private String externalPaymentUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
