package com.example.visceralmassageapi.finance;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FinanceExpenseIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger SUFFIX = new AtomicInteger(7000000);

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired OfficeRepository officeRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void ownerBootstrapProperties(DynamicPropertyRegistry registry) {
        registry.add("app.owner.bootstrap.enabled", () -> "true");
        registry.add("app.owner.bootstrap.phone", () -> OWNER_PHONE);
        registry.add("app.owner.bootstrap.email", () -> "OWNER@EXAMPLE.COM");
        registry.add("app.owner.bootstrap.password", () -> OWNER_PASSWORD);
    }

    @Test
    void financeManagerCanCreateAndListExpenses() throws Exception {
        Cookie[] financeCookies = loginCookies(OWNER_PHONE);
        long officeId = createOffice();

        mvc.perform(post("/api/admin/finance/expenses")
                        .with(csrf())
                        .cookie(financeCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount":1250.50,
                                  "category":" Materials ",
                                  "description":" Test expense ",
                                  "expenseDate":"2031-01-10",
                                  "officeId":%s
                                }
                                """.formatted(officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1250.50))
                .andExpect(jsonPath("$.category").value("Materials"))
                .andExpect(jsonPath("$.description").value("Test expense"))
                .andExpect(jsonPath("$.officeId").value(officeId));

        mvc.perform(get("/api/admin/finance/expenses")
                        .cookie(financeCookies)
                        .param("from", "2031-01-01")
                        .param("to", "2031-01-31")
                        .param("officeId", String.valueOf(officeId))
                        .param("sort", "expenseDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Test expense"));
    }

    @Test
    void expensesRequireFinanceRoleCsrfAndValidAmount() throws Exception {
        Cookie[] userCookies = loginCookies(createRegularUser());
        Cookie[] financeCookies = loginCookies(OWNER_PHONE);

        mvc.perform(get("/api/admin/finance/expenses").cookie(userCookies))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/finance/expenses")
                        .cookie(financeCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validExpenseJson()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/finance/expenses")
                        .with(csrf())
                        .cookie(financeCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount":0,
                                  "category":"Materials",
                                  "description":"Invalid amount",
                                  "expenseDate":"2031-01-10"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private long createOffice() {
        Office office = new Office();
        office.setName("Finance Office " + SUFFIX.incrementAndGet());
        office.setAddress("Kyiv");
        office.setActive(true);
        return officeRepository.save(office).getId();
    }

    private String createRegularUser() {
        String phone = "+38097" + SUFFIX.incrementAndGet();
        User user = new User();
        user.setPhone(phone);
        user.setFirstName("Finance");
        user.setLastName("User");
        user.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        user.getRoles().add(UserRole.USER);
        user.setEnabled(true);
        userRepository.save(user);
        return phone;
    }

    private Cookie[] loginCookies(String identifier) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}
                                """.formatted(identifier, OWNER_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookies();
    }

    private String validExpenseJson() {
        return """
                {
                  "amount":100,
                  "category":"Materials",
                  "description":"Test expense",
                  "expenseDate":"2031-01-10"
                }
                """;
    }
}
