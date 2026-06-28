package com.example.visceralmassageapi.site;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SiteSettingsIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger PHONE_SUFFIX = new AtomicInteger(6000000);
    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01, 0x02
    };

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void ownerBootstrapProperties(DynamicPropertyRegistry registry) {
        registry.add("app.owner.bootstrap.enabled", () -> "true");
        registry.add("app.owner.bootstrap.phone", () -> OWNER_PHONE);
        registry.add("app.owner.bootstrap.email", () -> "OWNER@EXAMPLE.COM");
        registry.add("app.owner.bootstrap.password", () -> OWNER_PASSWORD);
        registry.add("app.media.storage-directory", () -> "./build/test-media/site-settings");
        registry.add("app.media.max-file-size-bytes", () -> "32");
    }

    @Test
    void publicCanReadSettingsAndOnlyMasterCanUpdateThem() throws Exception {
        Cookie[] userCookies = loginCookies(createUser());
        Cookie[] masterCookies = loginCookies(OWNER_PHONE);

        mvc.perform(get("/api/site-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.footerBodyUa").isNotEmpty())
                .andExpect(jsonPath("$.footerBodyEn").isNotEmpty());

        mvc.perform(put("/api/admin/site-settings")
                        .with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"footerBodyUa":"Не має пройти"}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/admin/site-settings")
                        .with(csrf())
                        .cookie(masterCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "footerBodyUa":"Оновлений підвал",
                                  "footerBodyEn":"Updated footer",
                                  "homeIntroUa":"Головна: короткий вступ",
                                  "homeIntroEn":"Home intro",
                                  "homeBodyUa":"Повний CMS текст головної",
                                  "homeBodyEn":"Full CMS home body",
                                  "aboutBodyUa":"Про Ataraksia",
                                  "aboutBodyEn":"About Ataraksia",
                                  "contactBodyUa":"Контакти й графік",
                                  "contactBodyEn":"Contacts and schedule",
                                  "heroMediaUrls":" /api/media/1/content \\n/api/media/1/content\\n https://example.com/hero.jpg "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.footerBodyUa").value("Оновлений підвал"))
                .andExpect(jsonPath("$.homeBodyEn").value("Full CMS home body"))
                .andExpect(jsonPath("$.heroMediaUrls").value("/api/media/1/content\nhttps://example.com/hero.jpg"))
                .andExpect(jsonPath("$.updatedByUserId").exists());

        mvc.perform(get("/api/site-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.footerBodyUa").value("Оновлений підвал"))
                .andExpect(jsonPath("$.homeIntroEn").value("Home intro"))
                .andExpect(jsonPath("$.homeBodyUa").value("Повний CMS текст головної"))
                .andExpect(jsonPath("$.heroMediaUrls").value("/api/media/1/content\nhttps://example.com/hero.jpg"));
    }

    @Test
    void functionalRolesWithoutMasterCannotUpdateSiteSettings() throws Exception {
        Cookie[] smmCookies = loginCookies(createUser(UserRole.SMM));
        Cookie[] financeCookies = loginCookies(createUser(UserRole.FINANCE_MANAGER));
        Cookie[] specialistCookies = loginCookies(createUser(UserRole.SPECIALIST));

        mvc.perform(put("/api/admin/site-settings")
                        .with(csrf())
                        .cookie(smmCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"footerBodyUa":"SMM without MASTER"}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/admin/site-settings")
                        .with(csrf())
                        .cookie(financeCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"footerBodyUa":"Finance without MASTER"}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/admin/site-settings")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"footerBodyUa":"Specialist without MASTER"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void masterCanManageSiteSettingsMediaAndPublicCanReadLinkedContent() throws Exception {
        Cookie[] masterCookies = loginCookies(OWNER_PHONE);

        var firstUpload = mvc.perform(multipart("/api/admin/site-settings/media")
                        .file(validPng("first.png"))
                        .with(csrf())
                        .cookie(masterCookies))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.siteSettingsId").value(1))
                .andExpect(jsonPath("$.siteSliderSortOrder").value(0))
                .andReturn();
        String firstId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(firstUpload.getResponse().getContentAsString())
                .path("id")
                .asText();

        var secondUpload = mvc.perform(multipart("/api/admin/site-settings/media")
                        .file(validPng("second.png"))
                        .with(csrf())
                        .cookie(masterCookies))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.siteSliderSortOrder").value(1))
                .andReturn();
        String secondId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(secondUpload.getResponse().getContentAsString())
                .path("id")
                .asText();

        mvc.perform(put("/api/admin/site-settings/media/order")
                        .with(csrf())
                        .cookie(masterCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mediaIds":["%s","%s"]}
                                """.formatted(secondId, firstId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(secondId))
                .andExpect(jsonPath("$[1].id").value(firstId));

        mvc.perform(get("/api/site-settings/media"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(secondId))
                .andExpect(jsonPath("$[1].id").value(firstId));

        mvc.perform(get("/api/site-settings/media/{id}/content", firstId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG_BYTES));

        var contentUpload = mvc.perform(multipart("/api/admin/site-settings/content-media")
                        .file(validPng("inline.png"))
                        .with(csrf())
                        .cookie(masterCookies))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.siteSettingsId").value(1))
                .andExpect(jsonPath("$.siteSliderSortOrder").isEmpty())
                .andReturn();
        String contentId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(contentUpload.getResponse().getContentAsString())
                .path("id")
                .asText();

        mvc.perform(get("/api/site-settings/media"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(secondId))
                .andExpect(jsonPath("$[1].id").value(firstId))
                .andExpect(jsonPath("$[2]").doesNotExist());

        mvc.perform(get("/api/site-settings/media/{id}/content", contentId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG_BYTES));
    }

    private String createUser(UserRole... roles) {
        String phone = "+38099" + PHONE_SUFFIX.incrementAndGet();
        User user = new User();
        user.setPhone(phone);
        user.setFirstName("Site");
        user.setLastName("User");
        user.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        user.getRoles().add(UserRole.USER);
        for (UserRole role : roles) {
            user.getRoles().add(role);
        }
        user.setEnabled(true);
        userRepository.save(user);
        return phone;
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

    private MockMultipartFile validPng(String filename) {
        return new MockMultipartFile("file", filename, MediaType.IMAGE_PNG_VALUE, PNG_BYTES);
    }
}
