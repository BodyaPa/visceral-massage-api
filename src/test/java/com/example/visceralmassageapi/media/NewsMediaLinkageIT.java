package com.example.visceralmassageapi.media;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NewsMediaLinkageIT extends IntegrationTestBase {

    private static final String ADMIN_PHONE = "+380000000099";
    private static final String ADMIN_PASSWORD = "ConfiguredAdminPassword123!";
    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01, 0x02
    };

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource
    static void linkageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.admin.bootstrap.enabled", () -> "true");
        registry.add("app.admin.bootstrap.phone", () -> ADMIN_PHONE);
        registry.add("app.admin.bootstrap.email", () -> "ADMIN@EXAMPLE.COM");
        registry.add("app.admin.bootstrap.password", () -> ADMIN_PASSWORD);
        registry.add("app.media.storage-directory", () -> "./build/test-media/news-media-linkage");
    }

    @Test
    void onlyMediaLinkedToNewsIsPubliclyReadableAndLinkedMediaCannotBeDeleted() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();
        int newsId = createNews(adminCookies, "Media-linked news");
        String mediaId = uploadMedia(adminCookies);

        mvc.perform(get("/api/news/{newsId}/media/{mediaId}/content", newsId, mediaId))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/admin/news/{newsId}/media/{mediaId}", newsId, mediaId)
                        .with(csrf())
                        .cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(newsId));

        mvc.perform(put("/api/admin/news/{newsId}/cover/{mediaId}", newsId, mediaId)
                        .with(csrf())
                        .cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverMediaId").value(mediaId))
                .andExpect(jsonPath("$.coverDisplayMode").value("FILL"));

        mvc.perform(put("/api/admin/news/{newsId}/cover/display-mode/FIT", newsId)
                        .with(csrf())
                        .cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverDisplayMode").value("FIT"));

        mvc.perform(post("/api/admin/news/{newsId}/publish", newsId).with(csrf()).cookie(adminCookies))
                .andExpect(status().isOk());

        mvc.perform(get("/api/news/{newsId}", newsId).param("lang", "ua"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl")
                        .value("/api/news/" + newsId + "/media/" + mediaId + "/content"))
                .andExpect(jsonPath("$.coverImageAlt").value("Media-linked news"))
                .andExpect(jsonPath("$.coverDisplayMode").value("FIT"));

        mvc.perform(get("/api/admin/news/{newsId}/media", newsId).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(mediaId));

        mvc.perform(get("/api/news/{newsId}/media/{mediaId}/content", newsId, mediaId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG_BYTES));

        mvc.perform(delete("/api/admin/media/{mediaId}", mediaId).with(csrf()).cookie(adminCookies))
                .andExpect(status().isBadRequest());

        mvc.perform(delete("/api/admin/news/{newsId}/media/{mediaId}", newsId, mediaId)
                        .with(csrf())
                        .cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(Matchers.nullValue()));

        mvc.perform(get("/api/admin/news/{newsId}", newsId).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverMediaId").value(Matchers.nullValue()));

        mvc.perform(get("/api/news/{newsId}/media/{mediaId}/content", newsId, mediaId))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/admin/media/{mediaId}", mediaId).with(csrf()).cookie(adminCookies))
                .andExpect(status().isNoContent());
    }

    @Test
    void regularUserCannotLinkMediaToNews() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();
        int newsId = createNews(adminCookies, "Admin-only media link");
        String mediaId = uploadMedia(adminCookies);
        Cookie[] userCookies = registerUserCookies("+380000000094");

        mvc.perform(put("/api/admin/news/{newsId}/media/{mediaId}", newsId, mediaId)
                        .with(csrf())
                        .cookie(userCookies))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletingNewsMakesItsMediaPrivateAgain() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();
        int newsId = createNews(adminCookies, "Deleted news");
        String mediaId = uploadMedia(adminCookies);

        mvc.perform(put("/api/admin/news/{newsId}/media/{mediaId}", newsId, mediaId)
                        .with(csrf())
                        .cookie(adminCookies))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/admin/news/{newsId}", newsId).with(csrf()).cookie(adminCookies))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/news/{newsId}/media/{mediaId}/content", newsId, mediaId))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/admin/media/{mediaId}", mediaId).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newsId").value(Matchers.nullValue()));

        mvc.perform(delete("/api/admin/media/{mediaId}", mediaId).with(csrf()).cookie(adminCookies))
                .andExpect(status().isNoContent());
    }

    private int createNews(Cookie[] adminCookies, String title) throws Exception {
        var result = mvc.perform(post("/api/admin/news").with(csrf())
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titleUa":"%s","contentUa":"Content"}
                                """.formatted(title)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asInt();
    }

    private String uploadMedia(Cookie[] adminCookies) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "content.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES);
        var result = mvc.perform(multipart("/api/admin/media")
                        .file(file)
                        .with(csrf())
                        .cookie(adminCookies))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
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
}
