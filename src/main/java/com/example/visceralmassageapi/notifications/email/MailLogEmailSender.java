package com.example.visceralmassageapi.notifications.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev", "test"})
@ConditionalOnProperty(prefix = "app.notifications.email", name = "transport", havingValue = "log")
public class MailLogEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String text) {
        log.info("""

                ===== TEST EMAIL (not sent) =====
                to: {}
                subject: {}
                text:
                {}
                ================================
                """, to, subject, text);
    }
}
