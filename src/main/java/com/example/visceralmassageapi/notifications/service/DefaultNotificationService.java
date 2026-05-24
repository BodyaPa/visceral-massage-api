package com.example.visceralmassageapi.notifications.service;

import com.example.visceralmassageapi.notifications.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final EmailSender emailSender;

    @Override
    public void sendEmail(String to, String subject, String text) {
        emailSender.send(to, subject, text);
    }
}
