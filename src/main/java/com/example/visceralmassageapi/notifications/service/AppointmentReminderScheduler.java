package com.example.visceralmassageapi.notifications.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.reminders", name = "enabled", havingValue = "true")
public class AppointmentReminderScheduler {

    private final AppointmentReminderService reminderService;

    @Scheduled(fixedDelayString = "${app.notifications.reminders.fixed-delay-ms:300000}")
    public void sendDueReminders() {
        reminderService.sendDueReminders(OffsetDateTime.now());
    }
}
