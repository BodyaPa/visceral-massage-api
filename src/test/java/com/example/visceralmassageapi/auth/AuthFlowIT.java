package com.example.visceralmassageapi.auth;

import com.example.visceralmassageapi.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthFlowIT extends IntegrationTestBase {

    @Autowired MockMvc mvc;

    @Test
    void register_setsCookies() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"380000000001","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", notNullValue()))
                .andExpect(header().stringValues("Set-Cookie", everyItem(containsString("HttpOnly"))))
                .andExpect(header().stringValues("Set-Cookie", everyItem(containsString("SameSite=Lax"))))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void mutationWithoutCsrfToken_isRejected() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"380000000010","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_rejectsShortPassword() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"380000000011","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_rejectsPasswordMissingRequiredCharacterGroups() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"380000000014","email":null,"firstName":"Iryna","lastName":"Koval","password":"lowercaseonly12!"}
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"380000000015","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rdSecure"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_requiresPhoneOrEmailAndValidNames() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":null,"email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000013","email":null,"firstName":"<script>","lastName":"Koval","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_then_me_works_withCookies() throws Exception {
        // register
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000002","email":null,"firstName":"Олена","lastName":"Коваль","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk());

        // login
        var loginRes = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"+380000000002","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var cookies = loginRes.getResponse().getCookies();

        mvc.perform(get("/api/auth/me").cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+380000000002"))
                .andExpect(jsonPath("$.firstName").value("Олена"))
                .andExpect(jsonPath("$.lastName").value("Коваль"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void emailOnlyRegistration_canLoginByEmail() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":null,"email":"USER@EXAMPLE.COM","firstName":"Anna","lastName":"Lis","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.email").value("user@example.com"));

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"user@example.com","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anna"));
    }

    @Test
    void ukrainianLocalPhoneRegistration_canLoginWithLocalPhone() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"0671234567","email":null,"firstName":"Anna","lastName":"Lis","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+380671234567"));

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"0671234567","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+380671234567"));
    }

    @Test
    void loginDoesNotRevealWhetherIdentifierOrPasswordIsWrong() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":null,"email":"secure@example.com","firstName":"Anna","lastName":"Lis","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"missing@example.com","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"secure@example.com","password":"WrongPassw0rd!Secure"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void me_withoutCookies_isUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void refresh_updatesCookies() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000003","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk());

        var loginRes = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"+380000000003","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var cookies = loginRes.getResponse().getCookies();

        var originalRefresh = findCookie(cookies, "refresh_token");
        var refreshRes = mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(originalRefresh))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues("Set-Cookie", notNullValue()))
                .andReturn();

        var rotatedRefresh = findCookie(refreshRes.getResponse().getCookies(), "refresh_token");
        mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(originalRefresh))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(rotatedRefresh))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        var registerRes = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000004","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var refreshCookie = findCookie(registerRes.getResponse().getCookies(), "refresh_token");

        mvc.perform(post("/api/auth/logout").with(csrf()).cookie(refreshCookie))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(refreshCookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accessToken_cannotBeUsedAsRefreshToken() throws Exception {
        var registerRes = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000005","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var accessValue = findCookie(registerRes.getResponse().getCookies(), "access_token").getValue();

        mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(new Cookie("refresh_token", accessValue)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_cannotBeUsedAsAccessToken() throws Exception {
        var registerRes = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000006","email":null,"firstName":"Iryna","lastName":"Koval","password":"Passw0rd!Secure"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var refreshValue = findCookie(registerRes.getResponse().getCookies(), "refresh_token").getValue();

        mvc.perform(get("/api/auth/me").cookie(new Cookie("access_token", refreshValue)))
                .andExpect(status().isForbidden());
    }

    private Cookie findCookie(Cookie[] cookies, String name) {
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst()
                .orElseThrow();
    }
}
