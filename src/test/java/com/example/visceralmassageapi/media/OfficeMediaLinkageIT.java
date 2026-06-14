package com.example.visceralmassageapi.media;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.notifications.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Pattern;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OfficeMediaLinkageIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01, 0x02
    };

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean NotificationService notificationService;

    @DynamicPropertySource
    static void officeMediaProperties(DynamicPropertyRegistry registry) {
        registry.add("app.owner.bootstrap.enabled", () -> "true");
        registry.add("app.owner.bootstrap.phone", () -> OWNER_PHONE);
        registry.add("app.owner.bootstrap.email", () -> "OWNER@EXAMPLE.COM");
        registry.add("app.owner.bootstrap.password", () -> OWNER_PASSWORD);
        registry.add("app.media.storage-directory", () -> "./build/test-media/office-media-linkage");
    }

    @Test
    void officeMediaBecomesPublicOnlyAfterOfficeLinksIt() throws Exception {
        Cookie[] masterCookies = loginMasterCookies();
        long officeId = createOffice(masterCookies);
        String mediaId = uploadOfficeMedia(masterCookies);

        mvc.perform(get("/api/offices/{officeId}/media/{mediaId}/content", officeId, mediaId))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/admin/offices/{id}", officeId)
                        .with(csrf())
                        .cookie(masterCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Office Media Room",
                                  "address":"Kyiv, Media Street 1",
                                  "active":true,
                                  "directions":"Use the media entrance.",
                                  "googleMapsUrl":"https://maps.google.com/?q=Office+Media+Room",
                                  "photoMediaId":"%s",
                                  "videoMediaId":null
                                }
                                """.formatted(mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoMediaId").value(mediaId))
                .andExpect(jsonPath("$.photoMediaUrl").value("/api/offices/" + officeId + "/media/" + mediaId + "/content"))
                .andExpect(jsonPath("$.googleMapsUrl").value("https://maps.google.com/?q=Office+Media+Room"));

        mvc.perform(get("/api/offices/{officeId}/media/{mediaId}/content", officeId, mediaId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG_BYTES));

        mvc.perform(delete("/api/admin/media/{mediaId}", mediaId).with(csrf()).cookie(masterCookies))
                .andExpect(status().isBadRequest());
    }

    @Test
    void regularUserCannotUploadOfficeMedia() throws Exception {
        Cookie[] userCookies = registerUserCookies("+380000000093");

        mvc.perform(multipart("/api/admin/offices/media")
                        .file(validPng())
                        .with(csrf())
                        .cookie(userCookies))
                .andExpect(status().isForbidden());
    }

    private long createOffice(Cookie[] masterCookies) throws Exception {
        var result = mvc.perform(post("/api/admin/offices")
                        .with(csrf())
                        .cookie(masterCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Office Media Room",
                                  "address":"Kyiv, Media Street 1",
                                  "active":true,
                                  "directions":"Use the media entrance.",
                                  "googleMapsUrl":"https://maps.google.com/?q=Office+Media+Room"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private String uploadOfficeMedia(Cookie[] masterCookies) throws Exception {
        var result = mvc.perform(multipart("/api/admin/offices/media")
                        .file(validPng())
                        .with(csrf())
                        .cookie(masterCookies))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("office.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText();
    }

    private MockMultipartFile validPng() {
        return new MockMultipartFile("file", "office.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES);
    }

    private Cookie[] registerUserCookies(String phone) throws Exception {
        reset(notificationService);
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """.formatted(phone)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendSms(org.mockito.ArgumentMatchers.anyString(), bodyCaptor.capture());

        return mvc.perform(post("/api/auth/register/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"%s","code":"%s"}
                                """.formatted(phone, extractCode(bodyCaptor.getValue()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    private String extractCode(String body) {
        var matcher = Pattern.compile("\\b\\d{6}\\b").matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Message did not contain a 6-digit code");
        }
        return matcher.group();
    }

    private Cookie[] loginMasterCookies() throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}
                                """.formatted(OWNER_PHONE, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }
}
