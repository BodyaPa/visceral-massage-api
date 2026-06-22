package com.example.visceralmassageapi.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.schedule.fixed-events.archive", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FixedEventArchiveScheduler {

    private final FixedEventService fixedEventService;

    @Scheduled(fixedDelayString = "${app.schedule.fixed-events.archive.fixed-delay-ms:3600000}")
    public void deactivatePastEmptyEvents() {
        fixedEventService.deactivatePastEventsWithoutEnrollments(OffsetDateTime.now());
    }
}
