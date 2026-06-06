package com.example.visceralmassageapi.notifications.sms;

public interface SmsSender {
    void send(String to, String text);
}
