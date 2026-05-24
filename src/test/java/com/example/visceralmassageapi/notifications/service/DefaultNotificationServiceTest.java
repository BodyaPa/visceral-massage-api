package com.example.visceralmassageapi.notifications.service;

import com.example.visceralmassageapi.notifications.email.EmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultNotificationServiceTest {

    @Mock EmailSender emailSender;

    @InjectMocks DefaultNotificationService notificationService;

    @Test
    void sendEmailDelegatesToConfiguredEmailSender() {
        notificationService.sendEmail("client@example.com", "Subject", "Message");

        verify(emailSender).send("client@example.com", "Subject", "Message");
    }
}
