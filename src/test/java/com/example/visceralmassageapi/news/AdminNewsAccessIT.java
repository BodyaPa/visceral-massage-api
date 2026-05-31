package com.example.visceralmassageapi.news;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminNewsAccessIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";

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
    void smmCanCreateNewsButMasterWithoutSmmCannot() throws Exception {
        createUserWithRoles("+380000000092", UserRole.SMM);
        Cookie[] smmCookies = loginCookies("+380000000092");

        mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(smmCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("SMM news")))
                .andExpect(status().isOk());

        createUserWithRoles("+380000000091", UserRole.MASTER);
        Cookie[] masterCookies = loginCookies("+380000000091");

        mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(masterCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("Blocked master")))
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
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asInt();

        mvc.perform(get("/api/news/{id}", id).param("lang", "ua"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/news").param("lang", "ua"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(id)).doesNotExist());

        mvc.perform(put("/api/admin/news/{id}", id).with(csrf())
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("Admin news")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleUa").value("Admin news"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mvc.perform(get("/api/admin/news/{id}", id).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titleUa").value("Admin news"))
                .andExpect(jsonPath("$.titleEn").doesNotExist())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mvc.perform(post("/api/admin/news/{id}/publish", id).with(csrf()).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mvc.perform(get("/api/news/{id}", id).param("lang", "ua"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin news"));

        mvc.perform(get("/api/news/{id}", id).param("lang", "en"))
                .andExpect(status().isNotFound());

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
                .andExpect(jsonPath("$.title").value("Updated by admin"));

        mvc.perform(post("/api/admin/news/{id}/unpublish", id).with(csrf()).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mvc.perform(get("/api/news/{id}", id).param("lang", "ua"))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/admin/news/{id}/archive", id).with(csrf()).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mvc.perform(post("/api/admin/news/{id}/restore", id).with(csrf()).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mvc.perform(delete("/api/admin/news/{id}", id).with(csrf()).cookie(adminCookies))
                .andExpect(status().isBadRequest());

        var draftResult = mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        int draftId = objectMapper.readTree(draftResult.getResponse().getContentAsString()).path("id").asInt();

        mvc.perform(delete("/api/admin/news/{id}", draftId).with(csrf()).cookie(adminCookies))
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
    void draftCanRemainIncompleteButCannotBePublishedUntilTranslationIsComplete() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();

        var result = mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titleUa":"Only a title"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        int id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asInt();

        mvc.perform(post("/api/admin/news/{id}/publish", id).with(csrf()).cookie(adminCookies))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicListReturnsOnlyPublishedItemsWithRequestedTranslation() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();

        var result = mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody("Ukrainian only")))
                .andExpect(status().isOk())
                .andReturn();
        int id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asInt();

        mvc.perform(post("/api/admin/news/{id}/publish", id).with(csrf()).cookie(adminCookies))
                .andExpect(status().isOk());

        mvc.perform(get("/api/news").param("lang", "ua"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(id)).exists());

        mvc.perform(get("/api/news").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(id)).doesNotExist());
    }

    @Test
    void newsListsAreOrderedByCreationDateAfterOlderItemIsUpdated() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();

        int olderId = createPublishedNews(adminCookies, "Older news");
        int newerId = createPublishedNews(adminCookies, "Newer news");

        mvc.perform(patch("/api/admin/news/{id}", olderId).with(csrf())
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentUa":"Edited older content"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").exists());

        mvc.perform(get("/api/news")
                        .param("lang", "ua")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "updatedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(newerId))
                .andExpect(jsonPath("$.content[1].id").value(olderId));

        mvc.perform(get("/api/admin/news")
                        .cookie(adminCookies)
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "updatedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(newerId))
                .andExpect(jsonPath("$.content[1].id").value(olderId));
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
        return loginCookies(OWNER_PHONE);
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

    private void createUserWithRoles(String phone, UserRole... roles) {
        User user = new User();
        user.setPhone(phone);
        user.setFirstName("Role");
        user.setLastName("User");
        user.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        user.getRoles().add(UserRole.USER);
        for (UserRole role : roles) {
            user.getRoles().add(role);
        }
        user.setEnabled(true);
        userRepository.save(user);
    }

    private int createPublishedNews(Cookie[] adminCookies, String title) throws Exception {
        var result = mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newsBody(title)))
                .andExpect(status().isOk())
                .andReturn();
        int id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asInt();

        mvc.perform(post("/api/admin/news/{id}/publish", id).with(csrf()).cookie(adminCookies))
                .andExpect(status().isOk());

        return id;
    }

    private String newsBody(String title) {
        return """
                {"titleUa":"%s","contentUa":"Protected content"}
                """.formatted(title);
    }
}
