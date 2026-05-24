package com.example.visceralmassageapi.notifications.email;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpEmailSenderTest {

    @Test
    void sendBuildsMessageForJavaMailSender() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        SmtpEmailSender sender = new SmtpEmailSender(javaMailSender);
        ReflectionTestUtils.setField(sender, "from", "no-reply@ataraksia.test");

        sender.send("client@example.com", "Appointment", "Message body");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();

        assertThat(message.getFrom()).isEqualTo("no-reply@ataraksia.test");
        assertThat(message.getTo()).containsExactly("client@example.com");
        assertThat(message.getSubject()).isEqualTo("Appointment");
        assertThat(message.getText()).isEqualTo("Message body");
    }
}
