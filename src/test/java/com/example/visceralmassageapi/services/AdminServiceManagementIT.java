package com.example.visceralmassageapi.services;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminServiceManagementIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger PHONE_SUFFIX = new AtomicInteger(3000000);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void ownerBootstrapProperties(DynamicPropertyRegistry registry) {
        registry.add("app.owner.bootstrap.enabled", () -> "true");
        registry.add("app.owner.bootstrap.phone", () -> OWNER_PHONE);
        registry.add("app.owner.bootstrap.email", () -> "OWNER@EXAMPLE.COM");
        registry.add("app.owner.bootstrap.password", () -> OWNER_PASSWORD);
    }

    @Test
    void masterCanCreateListViewAndUpdateServices() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);

        var createResult = mvc.perform(post("/api/admin/services")
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleUa":"Вісцеральний масаж",
                                  "descriptionUa":"Базова процедура",
                                  "titleEn":"Visceral massage",
                                  "descriptionEn":"Base service",
                                  "durationMinutes":60,
                                  "basePrice":1200.00,
                                  "active":true,
                                  "externalPaymentUrl":"https://pay.example.com/service-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleUa").value("Вісцеральний масаж"))
                .andExpect(jsonPath("$.basePrice").value(1200.00))
                .andReturn();

        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mvc.perform(get("/api/admin/services")
                        .cookie(ownerCookies)
                        .param("query", "масаж")
                        .param("active", "true")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));

        mvc.perform(get("/api/admin/services/{id}", id).cookie(ownerCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(60));

        mvc.perform(put("/api/admin/services/{id}", id)
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleUa":"Вісцеральний масаж оновлений",
                                  "descriptionUa":"Оновлений опис",
                                  "titleEn":null,
                                  "descriptionEn":null,
                                  "durationMinutes":75,
                                  "basePrice":1400.00,
                                  "active":false,
                                  "externalPaymentUrl":null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleUa").value("Вісцеральний масаж оновлений"))
                .andExpect(jsonPath("$.durationMinutes").value(75))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.externalPaymentUrl").doesNotExist());
    }

    @Test
    void publicServicesReturnOnlyActiveCompleteRequestedLocale() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);
        long activeId = createService(ownerCookies, "Публічна послуга", "Public service", true);
        long uaOnlyId = createService(ownerCookies, "Тільки українською", null, true);
        createService(ownerCookies, "Неактивна", "Inactive", false);

        mvc.perform(get("/api/services")
                        .param("lang", "ua")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(activeId)).exists())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(uaOnlyId)).exists());

        mvc.perform(get("/api/services")
                        .param("lang", "en")
                        .param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(activeId)).exists())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(uaOnlyId)).doesNotExist());

        mvc.perform(get("/api/services").param("lang", "pl"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonMasterCannotManageServices() throws Exception {
        String smmPhone = uniquePhone();
        createUserWithRoles(smmPhone, UserRole.SMM);
        Cookie[] smmCookies = loginCookies(smmPhone);

        mvc.perform(get("/api/admin/services").cookie(smmCookies))
                .andExpect(status().isForbidden());
    }

    @Test
    void serviceMutationsRequireCsrfAndValidRequiredFields() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);

        mvc.perform(post("/api/admin/services")
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validServiceBody("Без CSRF", "No CSRF", true)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/services")
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titleUa":"","durationMinutes":0,"basePrice":-1}
                                """))
                .andExpect(status().isBadRequest());
    }

    private long createService(Cookie[] ownerCookies, String titleUa, String titleEn, boolean active) throws Exception {
        var result = mvc.perform(post("/api/admin/services")
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validServiceBody(titleUa, titleEn, active)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private String validServiceBody(String titleUa, String titleEn, boolean active) {
        String titleEnValue = titleEn == null ? "null" : "\"%s\"".formatted(titleEn);
        return """
                {
                  "titleUa":"%s",
                  "descriptionUa":"Опис",
                  "titleEn":%s,
                  "descriptionEn":null,
                  "durationMinutes":45,
                  "basePrice":900.00,
                  "active":%s,
                  "externalPaymentUrl":null
                }
                """.formatted(titleUa, titleEnValue, active);
    }

    private void createUserWithRoles(String phone, UserRole... roles) {
        User user = new User();
        user.setPhone(phone);
        user.setFirstName("Service");
        user.setLastName("User");
        user.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        user.getRoles().add(UserRole.USER);
        for (UserRole role : roles) {
            user.getRoles().add(role);
        }
        user.setEnabled(true);
        userRepository.save(user);
    }

    private Cookie[] loginCookies(String phone) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}
                                """.formatted(phone, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    private String uniquePhone() {
        return "+38097" + PHONE_SUFFIX.incrementAndGet();
    }
}
