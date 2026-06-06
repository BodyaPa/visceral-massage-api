package com.example.visceralmassageapi.notifications.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.notifications.sms", name = "transport",
        havingValue = "disabled", matchIfMissing = true)
public class DisabledSmsSender implements SmsSender {

    @Override
    public void send(String to, String text) {
        throw new IllegalStateException("SMS transport is not configured.");
    }
}
