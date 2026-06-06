package com.example.visceralmassageapi.notifications.service;

import com.example.visceralmassageapi.notifications.email.EmailSender;
import com.example.visceralmassageapi.notifications.sms.SmsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final EmailSender emailSender;
    private final SmsSender smsSender;

    @Override
    public void sendEmail(String to, String subject, String text) {
        emailSender.send(to, subject, text);
    }

    @Override
    public void sendSms(String to, String text) {
        smsSender.send(to, text);
    }
}
