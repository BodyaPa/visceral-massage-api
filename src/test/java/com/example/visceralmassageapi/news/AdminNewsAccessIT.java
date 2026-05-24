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
        mvc.perform(get("/api/news"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotCreateNewsThroughAdminEndpoint() throws Exception {
        mvc.perform(post("/api/admin/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("Blocked anonymous")))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotCreateNewsThroughAdminEndpoint() throws Exception {
        Cookie[] userCookies = registerUserCookies("+380000000097");

        mvc.perform(post("/api/admin/news")
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("Blocked user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCannotCreateNewsThroughPublicEndpoint() throws Exception {
        Cookie[] userCookies = registerUserCookies("+380000000096");

        mvc.perform(post("/api/news")
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("No public mutation")))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void adminCanManageNewsThroughAdminEndpoint() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();

        var result = mvc.perform(post("/api/admin/news")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("Admin news")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin news"))
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asInt();

        mvc.perform(patch("/api/admin/news/{id}", id)
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated by admin"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated by admin"));

        mvc.perform(delete("/api/admin/news/{id}", id).cookie(adminCookies))
                .andExpect(status().isNoContent());
    }

    private Cookie[] registerUserCookies(String phone) throws Exception {
        return mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","email":null,"password":"Passw0rd!"}
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    private Cookie[] loginAdminCookies() throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","password":"%s"}
                                """.formatted(ADMIN_PHONE, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    private String newsBody(String title) {
        return """
                {"title":"%s","content":"Protected content"}
                """.formatted(title);
    }
}
