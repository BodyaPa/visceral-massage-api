package com.example.visceralmassageapi.audit;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditLoggingIT extends IntegrationTestBase {

    @Autowired MockMvc mvc;

    @MockitoBean AuditLogger auditLogger;

    @Test
    void registrationAndLoginRecordAuditEvents() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000088","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk());

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
        Cookie[] userCookies = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000087","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
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
}
