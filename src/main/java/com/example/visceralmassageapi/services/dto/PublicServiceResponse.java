package com.example.visceralmassageapi.services.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import com.example.visceralmassageapi.services.entity.ServiceBookingMode;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PublicServiceResponse {
    private Long id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private BigDecimal basePrice;
    private ServiceBookingMode bookingMode;
}
