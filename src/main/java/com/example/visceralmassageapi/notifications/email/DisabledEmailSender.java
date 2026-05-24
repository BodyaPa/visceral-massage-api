package com.example.visceralmassageapi.notifications.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.notifications.email", name = "transport",
        havingValue = "disabled", matchIfMissing = true)
public class DisabledEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String text) {
        throw new IllegalStateException("Email transport is not configured.");
    }
}
