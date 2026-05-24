package com.example.visceralmassageapi.auth;

import com.example.visceralmassageapi.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthFlowIT extends IntegrationTestBase {

    @Autowired MockMvc mvc;

    @Test
    void register_setsCookies() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"380000000001","email":null,"password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", notNullValue())); // мінімальний smoke
    }

    @Test
    void login_then_me_works_withCookies() throws Exception {
        // register
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000002","email":null,"password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk());

        // login
        var loginRes = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000002","password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var cookies = loginRes.getResponse().getCookies();

        mvc.perform(get("/api/auth/me").cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+380000000002"));
    }

    @Test
    void me_withoutCookies_isUnauthorized() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void refresh_updatesCookies() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000003","email":null,"password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk());

        var loginRes = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000003","password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var cookies = loginRes.getResponse().getCookies();

        var originalRefresh = findCookie(cookies, "refresh_token");
        var refreshRes = mvc.perform(post("/api/auth/refresh").cookie(originalRefresh))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues("Set-Cookie", notNullValue()))
                .andReturn();

        var rotatedRefresh = findCookie(refreshRes.getResponse().getCookies(), "refresh_token");
        mvc.perform(post("/api/auth/refresh").cookie(originalRefresh))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/refresh").cookie(rotatedRefresh))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_revokesRefreshToken() throws Exception {
        var registerRes = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000004","email":null,"password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var refreshCookie = findCookie(registerRes.getResponse().getCookies(), "refresh_token");

        mvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accessToken_cannotBeUsedAsRefreshToken() throws Exception {
        var registerRes = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000005","email":null,"password":"Passw0rd!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        var accessValue = findCookie(registerRes.getResponse().getCookies(), "access_token").getValue();

        mvc.perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", accessValue)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_cannotBeUsedAsAccessToken() throws Exception {
        var registerRes = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"+380000000006","email":null,"password":"Passw0rd!"}
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
