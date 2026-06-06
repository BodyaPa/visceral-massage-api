package com.example.visceralmassageapi.audit;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.notifications.service.NotificationService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditLoggingIT extends IntegrationTestBase {

    @Autowired MockMvc mvc;

    @MockitoBean AuditLogger auditLogger;
    @MockitoBean NotificationService notificationService;

    @Test
    void registrationAndLoginRecordAuditEvents() throws Exception {
        registerCookies("+380000000088");

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"+380000000088","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"+380000000088","password":"WrongPassw0rd!Secure"}
                                """))
                .andExpect(status().isBadRequest());

        verify(auditLogger).userRegistered(anyLong());
        verify(auditLogger).loginSucceeded(anyLong());
        verify(auditLogger).loginFailed();
    }

    @Test
    void regularUserDeniedFromAdminEndpointRecordsAuditEvent() throws Exception {
        Cookie[] userCookies = registerCookies("+380000000087");
        reset(auditLogger);

        mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titleUa":"Not persisted","contentUa":"Not persisted"}
                                """))
                .andExpect(status().isForbidden());

        verify(auditLogger).adminAccessDenied("POST");
    }

    private Cookie[] registerCookies(String phone) throws Exception {
        reset(notificationService);
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """.formatted(phone)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendSms(org.mockito.ArgumentMatchers.anyString(), bodyCaptor.capture());

        return mvc.perform(post("/api/auth/register/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","code":"%s"}
                                """.formatted(phone, extractCode(bodyCaptor.getValue()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    private String extractCode(String body) {
        var matcher = Pattern.compile("\\b\\d{6}\\b").matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Message did not contain a 6-digit code");
        }
        return matcher.group();
    }
}
