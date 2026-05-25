package com.example.visceralmassageapi.news;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminNewsAccessIT extends IntegrationTestBase {

    private static final String ADMIN_PHONE = "+380000000099";
    private static final String ADMIN_PASSWORD = "ConfiguredAdminPassword123!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void adminBootstrapProperties(DynamicPropertyRegistry registry) {
        registry.add("app.admin.bootstrap.enabled", () -> "true");
        registry.add("app.admin.bootstrap.phone", () -> ADMIN_PHONE);
        registry.add("app.admin.bootstrap.email", () -> "ADMIN@EXAMPLE.COM");
        registry.add("app.admin.bootstrap.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void publicNewsRead_isAvailableWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/news").param("lang", "ua"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotCreateNewsThroughAdminEndpoint() throws Exception {
        mvc.perform(post("/api/admin/news").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("Blocked anonymous")))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotCreateNewsThroughAdminEndpoint() throws Exception {
        Cookie[] userCookies = registerUserCookies("+380000000097");

        mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("Blocked user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReadFullNewsTranslationsThroughAdminEndpoint() throws Exception {
        mvc.perform(get("/api/admin/news"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCannotCreateNewsThroughPublicEndpoint() throws Exception {
        Cookie[] userCookies = registerUserCookies("+380000000096");

        mvc.perform(post("/api/news").with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("No public mutation")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void adminCanManageNewsThroughAdminEndpoint() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();

        var result = mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("Admin news")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleUa").value("Admin news"))
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asInt();

        mvc.perform(get("/api/admin/news/{id}", id).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleUa").value("Admin news"))
                .andExpect(jsonPath("$.titleEn").doesNotExist());

        mvc.perform(get("/api/news/{id}", id).param("lang", "ua"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin news"))
                .andExpect(jsonPath("$.translationAvailable").value(true));

        mvc.perform(get("/api/news/{id}", id).param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translationAvailable").value(false));

        mvc.perform(patch("/api/admin/news/{id}", id).with(csrf())
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titleEn":"Updated by admin","contentEn":"English content"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleEn").value("Updated by admin"));

        mvc.perform(get("/api/news/{id}", id).param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated by admin"))
                .andExpect(jsonPath("$.translationAvailable").value(true));

        mvc.perform(delete("/api/admin/news/{id}", id).with(csrf()).cookie(adminCookies))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminMutationWithoutCsrfToken_isRejected() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();

        mvc.perform(post("/api/admin/news")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("Blocked CSRF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotCreateNewsWithIncompleteTranslation() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();

        mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titleUa":"Only a title"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicNewsRejectsUnsupportedLocale() throws Exception {
        mvc.perform(get("/api/news").param("lang", "pl"))
                .andExpect(status().isBadRequest());
    }

    private Cookie[] registerUserCookies(String phone) throws Exception {
        return mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    private Cookie[] loginAdminCookies() throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}
                                """.formatted(ADMIN_PHONE, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    private String newsBody(String title) {
        return """
                {"titleUa":"%s","contentUa":"Protected content"}
                """.formatted(title);
    }
}
