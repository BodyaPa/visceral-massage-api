package com.example.visceralmassageapi.auth;

import com.example.visceralmassageapi.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

        mvc.perform(post("/api/auth/refresh").cookie(cookies))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues("Set-Cookie", notNullValue()));
    }
}