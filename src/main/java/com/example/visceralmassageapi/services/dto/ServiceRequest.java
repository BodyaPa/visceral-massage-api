package com.example.visceralmassageapi.services.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ServiceRequest {
    @NotBlank
    @Size(max = 160)
    private String titleUa;

    @Size(max = 10000)
    private String descriptionUa;

    @Size(max = 160)
    private String titleEn;

    @Size(max = 10000)
    private String descriptionEn;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal basePrice;

    private boolean active = true;

    @Size(max = 1000)
    private String externalPaymentUrl;
}
