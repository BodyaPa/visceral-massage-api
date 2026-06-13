package com.example.visceralmassageapi.common.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.schedule")
public class ScheduleProps {

    @Min(0)
    private int appointmentBufferMinutes = 30;
}
