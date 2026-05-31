package com.example.visceralmassageapi.auth;

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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminUserManagementIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger PHONE_SUFFIX = new AtomicInteger(1000000);

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
    void masterCanListFilterViewAndUpdateUserRoles() throws Exception {
        String email = "roleuser-" + PHONE_SUFFIX.get() + "@example.com";
        User user = createUserWithRoles(uniquePhone(), email, UserRole.SMM);
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);

        mvc.perform(get("/api/admin/users")
                        .cookie(ownerCookies)
                        .param("query", "roleuser")
                        .param("role", "SMM")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(user.getId()))
                .andExpect(jsonPath("$.content[0].roles", containsInAnyOrder("SMM", "USER")));

        mvc.perform(get("/api/admin/users/{id}", user.getId()).cookie(ownerCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mvc.perform(patch("/api/admin/users/{id}/roles", user.getId())
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roles":["SPECIALIST","FINANCE_MANAGER"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER", "SPECIALIST", "FINANCE_MANAGER")));
    }

    @Test
    void masterCannotRemoveOwnMasterRole() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);
        long ownerId = userRepository.findByPhone(OWNER_PHONE).orElseThrow().getId();

        mvc.perform(patch("/api/admin/users/{id}/roles", ownerId)
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roles":["USER","SMM"]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonMasterCannotManageUsers() throws Exception {
        String smmPhone = uniquePhone();
        createUserWithRoles(smmPhone, "smm-only-" + PHONE_SUFFIX.get() + "@example.com", UserRole.SMM);
        Cookie[] smmCookies = loginCookies(smmPhone);

        mvc.perform(get("/api/admin/users").cookie(smmCookies))
                .andExpect(status().isForbidden());
    }

    private User createUserWithRoles(String phone, String email, UserRole... roles) {
        User user = new User();
        user.setPhone(phone);
        user.setEmail(email);
        user.setFirstName("Role");
        user.setLastName("User");
        user.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        user.getRoles().add(UserRole.USER);
        for (UserRole role : roles) {
            user.getRoles().add(role);
        }
        user.setEnabled(true);
        return userRepository.save(user);
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
        return "+38099" + PHONE_SUFFIX.incrementAndGet();
    }
}
