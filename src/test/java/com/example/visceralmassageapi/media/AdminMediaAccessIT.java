package com.example.visceralmassageapi.media;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.notifications.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMediaAccessIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger PHONE_SUFFIX = new AtomicInteger(8000000);
    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01, 0x02
    };

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean NotificationService notificationService;

    @DynamicPropertySource
    static void mediaProperties(DynamicPropertyRegistry registry) {
        registry.add("app.owner.bootstrap.enabled", () -> "true");
        registry.add("app.owner.bootstrap.phone", () -> OWNER_PHONE);
        registry.add("app.owner.bootstrap.email", () -> "OWNER@EXAMPLE.COM");
        registry.add("app.owner.bootstrap.password", () -> OWNER_PASSWORD);
        registry.add("app.media.storage-directory", () -> "./build/test-media/admin-media-access");
        registry.add("app.media.max-file-size-bytes", () -> "16");
    }

    @Test
    void anonymousAndRegularUsersCannotUploadMedia() throws Exception {
        mvc.perform(multipart("/api/admin/media").file(validPng()).with(csrf()))
                .andExpect(status().isForbidden());

        Cookie[] userCookies = registerUserCookies("+380000000095");

        mvc.perform(multipart("/api/admin/media").file(validPng()).with(csrf()).cookie(userCookies))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUploadRequiresCsrfProtection() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();

        mvc.perform(multipart("/api/admin/media").file(validPng()).cookie(adminCookies))
                .andExpect(status().isForbidden());
    }

    @Test
    void smmCanUploadMediaButMasterWithoutSmmCannot() throws Exception {
        User smm = createUserWithRoles(UserRole.SMM);
        Cookie[] smmCookies = loginCookies(smm.getPhone());

        mvc.perform(multipart("/api/admin/media")
                        .file(validPng())
                        .with(csrf())
                        .cookie(smmCookies))
                .andExpect(status().isCreated());

        User masterOnly = createUserWithRoles(UserRole.MASTER);
        Cookie[] masterCookies = loginCookies(masterOnly.getPhone());

        mvc.perform(multipart("/api/admin/media")
                        .file(validPng())
                        .with(csrf())
                        .cookie(masterCookies))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUploadPreviewListAndDeletePrivateMedia() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();

        var uploadResult = mvc.perform(multipart("/api/admin/media")
                        .file(validPng())
                        .with(csrf())
                        .cookie(adminCookies))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("cover.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(PNG_BYTES.length))
                .andReturn();

        String id = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).path("id").asText();

        mvc.perform(get("/api/admin/media/{id}", id).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mvc.perform(get("/api/admin/media").cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(id)).exists());

        mvc.perform(get("/api/admin/media/{id}/content", id).cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG_BYTES));

        mvc.perform(delete("/api/admin/media/{id}", id).with(csrf()).cookie(adminCookies))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/media/{id}", id).cookie(adminCookies))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCannotUploadContentThatDoesNotMatchDeclaredType() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();
        MockMultipartFile fakePng = new MockMultipartFile(
                "file", "fake.png", MediaType.IMAGE_PNG_VALUE, "not a png".getBytes()
        );

        mvc.perform(multipart("/api/admin/media")
                        .file(fakePng)
                        .with(csrf())
                        .cookie(adminCookies))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCannotUploadMediaAboveConfiguredLimit() throws Exception {
        Cookie[] adminCookies = loginAdminCookies();
        byte[] oversizedBytes = Arrays.copyOf(PNG_BYTES, 17);
        MockMultipartFile oversized = new MockMultipartFile(
                "file", "large.png", MediaType.IMAGE_PNG_VALUE, oversizedBytes
        );

        mvc.perform(multipart("/api/admin/media")
                        .file(oversized)
                        .with(csrf())
                        .cookie(adminCookies))
                .andExpect(status().isPayloadTooLarge());
    }

    private MockMultipartFile validPng() {
        return new MockMultipartFile("file", "cover.png", MediaType.IMAGE_PNG_VALUE, PNG_BYTES);
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

    private User createUserWithRoles(UserRole... roles) {
        User user = new User();
        user.setPhone("+38098" + PHONE_SUFFIX.incrementAndGet());
        user.setFirstName("Media");
        user.setLastName("User");
        user.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        user.getRoles().add(UserRole.USER);
        for (UserRole role : roles) {
            user.getRoles().add(role);
        }
        user.setEnabled(true);
        return userRepository.save(user);
    }
}
