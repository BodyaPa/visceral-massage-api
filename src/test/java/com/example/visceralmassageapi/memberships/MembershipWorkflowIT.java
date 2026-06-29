package com.example.visceralmassageapi.memberships;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.services.entity.ServiceBookingMode;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import com.example.visceralmassageapi.services.repository.ServiceOfferingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MembershipWorkflowIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger SUFFIX = new AtomicInteger(8000000);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean AuditLogger auditLogger;

    @DynamicPropertySource
    static void ownerBootstrapProperties(DynamicPropertyRegistry registry) {
        registry.add("app.owner.bootstrap.enabled", () -> "true");
        registry.add("app.owner.bootstrap.phone", () -> OWNER_PHONE);
        registry.add("app.owner.bootstrap.email", () -> "OWNER@EXAMPLE.COM");
        registry.add("app.owner.bootstrap.password", () -> OWNER_PASSWORD);
    }

    @Test
    void userCreatesPurchaseAndFinanceManagerConfirmsPayment() throws Exception {
        String userPhone = createUser();
        Cookie[] userCookies = loginCookies(userPhone);
        Cookie[] financeCookies = loginCookies(OWNER_PHONE);
        long userId = userRepository.findByPhone(userPhone).orElseThrow().getId();
        long financeId = userRepository.findByPhone(OWNER_PHONE).orElseThrow().getId();
        reset(auditLogger);

        var offersResult = mvc.perform(get("/api/memberships/offers").cookie(userCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("care-4"))
                .andReturn();
        long offerId = objectMapper.readTree(offersResult.getResponse().getContentAsString()).path(0).path("id").asLong();

        var purchaseResult = mvc.perform(post("/api/memberships/purchases")
                        .with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"offerId":%s}
                                """.formatted(offerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT_CONFIRMATION"))
                .andExpect(jsonPath("$.offerCode").value("care-4"))
                .andExpect(jsonPath("$.visitsRemaining").value(4))
                .andReturn();
        long purchaseId = objectMapper.readTree(purchaseResult.getResponse().getContentAsString()).path("id").asLong();
        verify(auditLogger).membershipPurchaseCreated(purchaseId, userId);

        mvc.perform(post("/api/memberships/purchases/%s/payment-session".formatted(purchaseId))
                        .with(csrf())
                        .cookie(userCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchaseId").value(purchaseId))
                .andExpect(jsonPath("$.mode").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.requiresManualConfirmation").value(true));

        mvc.perform(get("/api/memberships/purchases/my")
                        .cookie(userCookies)
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(purchaseId));

        mvc.perform(get("/api/admin/finance/memberships")
                        .cookie(financeCookies)
                        .param("status", "AWAITING_PAYMENT_CONFIRMATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(purchaseId));

        mvc.perform(post("/api/admin/finance/memberships/%s/confirm-payment".formatted(purchaseId))
                        .with(csrf())
                        .cookie(financeCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.activatedAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.confirmedByUserId").value(financeId));
        verify(auditLogger).membershipPurchasePaymentConfirmed(purchaseId, financeId);
    }

    @Test
    void membershipFinanceEndpointsRequireFinanceRoleAndCsrf() throws Exception {
        Cookie[] userCookies = loginCookies(createUser());
        Cookie[] financeCookies = loginCookies(OWNER_PHONE);

        mvc.perform(get("/api/admin/finance/memberships").cookie(userCookies))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/memberships/purchases")
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"offerId":1}
                                """))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/finance/memberships/1/confirm-payment")
                        .cookie(financeCookies))
                .andExpect(status().isForbidden());
    }

    @Test
    void masterUpdatesMembershipOfferEligibility() throws Exception {
        Cookie[] masterCookies = loginCookies(OWNER_PHONE);
        Cookie[] userCookies = loginCookies(createUser());
        ServiceOffering service = createService();

        var offersResult = mvc.perform(get("/api/admin/memberships/offers").cookie(masterCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("care-4"))
                .andReturn();
        long offerId = objectMapper.readTree(offersResult.getResponse().getContentAsString()).path(0).path("id").asLong();

        mvc.perform(put("/api/admin/memberships/offers/%s".formatted(offerId))
                        .with(csrf())
                        .cookie(masterCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleUa":"Абонемент 4 візити",
                                  "titleEn":"Care pack 4",
                                  "descriptionUa":"Оновлений опис",
                                  "descriptionEn":"Updated description",
                                  "price":4200,
                                  "visitsTotal":4,
                                  "validityDays":60,
                                  "active":true,
                                  "eligibleServiceIds":[%s]
                                }
                                """.formatted(service.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibleServiceIds[0]").value(service.getId()))
                .andExpect(jsonPath("$.validityDays").value(60));

        mvc.perform(get("/api/admin/memberships/offers").cookie(userCookies))
                .andExpect(status().isForbidden());
    }

    private String createUser() {
        String phone = "+38095" + SUFFIX.incrementAndGet();
        User user = new User();
        user.setPhone(phone);
        user.setFirstName("Membership");
        user.setLastName("User");
        user.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        user.getRoles().add(UserRole.USER);
        user.setEnabled(true);
        userRepository.save(user);
        return phone;
    }

    private ServiceOffering createService() {
        ServiceOffering service = new ServiceOffering();
        service.setTitleUa("Тестова послуга");
        service.setTitleEn("Test service");
        service.setDurationMinutes(60);
        service.setBasePrice(java.math.BigDecimal.valueOf(1200));
        service.setBookingMode(ServiceBookingMode.INDIVIDUAL_APPOINTMENT);
        service.setActive(true);
        return serviceOfferingRepository.save(service);
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

}
