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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditLoggingIT extends IntegrationTestBase {

    @Autowired MockMvc mvc;

    @MockitoBean AuditLogger auditLogger;

    @Test
    void registrationAndLoginRecordAuditEvents() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000088","email":null,"password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000088","password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000088","password":"WrongPassw0rd!"}
                                """))
                .andExpect(status().isBadRequest());

        verify(auditLogger).userRegistered(anyLong());
        verify(auditLogger).loginSucceeded(anyLong());
        verify(auditLogger).loginFailed();
    }

    @Test
    void regularUserDeniedFromAdminEndpointRecordsAuditEvent() throws Exception {
        Cookie[] userCookies = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000087","email":null,"password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
        reset(auditLogger);

        mvc.perform(post("/api/admin/news")
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Not persisted","content":"Not persisted"}
                                """))
                .andExpect(status().isForbidden());

        verify(auditLogger).adminAccessDenied("POST");
    }
}
