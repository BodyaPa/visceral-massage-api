package com.example.visceralmassageapi.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"dev", "test"})
public class DevLogNotificationService implements NotificationService {

    @Override
    public void sendEmail(String to, String subject, String text) {
        log.info("""
                
                ===== DEV EMAIL (not sent) =====
                to: {}
                subject: {}
                text:
                {}
                ================================
                """, to, subject, text);
    }
}