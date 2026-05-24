package com.example.visceralmassageapi.notifications.email;

public interface EmailSender {
    void send(String to, String subject, String text);
}
