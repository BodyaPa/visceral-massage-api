package com.example.visceralmassageapi.notifications.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class ProdDisabledNotificationService implements NotificationService {
    @Override
    public void sendEmail(String to, String subject, String text) {
        throw new IllegalStateException("Email is not configured for prod yet.");
    }
}