package com.example.visceralmassageapi.notifications.service;

public interface NotificationService {
    void sendEmail(String to, String subject, String text);
}