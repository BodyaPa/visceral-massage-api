package com.example.visceralmassageapi.offices;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
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

class AdminOfficeManagementIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger PHONE_SUFFIX = new AtomicInteger(2000000);

    @Autowired MockMvc mvc;
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
    void masterCanCreateListViewAndUpdateOffices() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);

        var createResult = mvc.perform(post("/api/admin/offices")
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Office 1",
                                  "address":"Kyiv, Central Street 1",
                                  "active":true,
                                  "phone":"+380441112233",
                                  "email":"OFFICE@EXAMPLE.COM",
                                  "locationDetails":"Second floor"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Office 1"))
                .andExpect(jsonPath("$.email").value("office@example.com"))
                .andReturn();

        long id = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(createResult.getResponse().getContentAsString())
                .path("id")
                .asLong();

        mvc.perform(get("/api/admin/offices")
                        .cookie(ownerCookies)
                        .param("query", "central")
                        .param("active", "true")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id));

        mvc.perform(get("/api/admin/offices/{id}", id).cookie(ownerCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Kyiv, Central Street 1"));

        mvc.perform(put("/api/admin/offices/{id}", id)
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Office 1 Renovated",
                                  "address":"Kyiv, Central Street 2",
                                  "active":false,
                                  "phone":null,
                                  "email":null,
                                  "locationDetails":"Entrance from the courtyard"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Office 1 Renovated"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void nonMasterCannotManageOffices() throws Exception {
        String smmPhone = uniquePhone();
        createUserWithRoles(smmPhone, UserRole.SMM);
        Cookie[] smmCookies = loginCookies(smmPhone);

        mvc.perform(get("/api/admin/offices").cookie(smmCookies))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicOfficesReturnOnlyActiveOffices() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);
        long activeId = createOffice(ownerCookies, "Public Office", true);
        long inactiveId = createOffice(ownerCookies, "Hidden Office", false);

        mvc.perform(get("/api/offices").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(activeId)).exists())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(inactiveId)).doesNotExist());
    }

    @Test
    void officeMutationsRequireCsrfAndValidRequiredFields() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);

        mvc.perform(post("/api/admin/offices")
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Office 2","address":"Lviv"}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/offices")
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","address":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    private void createUserWithRoles(String phone, UserRole... roles) {
        User user = new User();
        user.setPhone(phone);
        user.setFirstName("Office");
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

    private long createOffice(Cookie[] ownerCookies, String name, boolean active) throws Exception {
        var result = mvc.perform(post("/api/admin/offices")
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "address":"Kyiv",
                                  "active":%s,
                                  "phone":null,
                                  "email":null,
                                  "locationDetails":null
                                }
                                """.formatted(name, active)))
                .andExpect(status().isOk())
                .andReturn();

        return com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(result.getResponse().getContentAsString())
                .path("id")
                .asLong();
    }

    private String uniquePhone() {
        return "+38098" + PHONE_SUFFIX.incrementAndGet();
    }
}
