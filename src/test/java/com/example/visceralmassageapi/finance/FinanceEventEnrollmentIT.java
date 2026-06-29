package com.example.visceralmassageapi.finance;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.schedule.domain.FixedEvent;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollment;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;
import com.example.visceralmassageapi.schedule.repository.FixedEventEnrollmentRepository;
import com.example.visceralmassageapi.schedule.repository.FixedEventRepository;
import com.example.visceralmassageapi.services.entity.ServiceBookingMode;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import com.example.visceralmassageapi.services.repository.ServiceOfferingRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FinanceEventEnrollmentIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger SUFFIX = new AtomicInteger(9100000);

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired FixedEventRepository fixedEventRepository;
    @Autowired FixedEventEnrollmentRepository enrollmentRepository;
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
    void financeManagerCanListAndConfirmEventEnrollmentPayment() throws Exception {
        Cookie[] financeCookies = loginCookies(OWNER_PHONE);
        long actorId = userRepository.findByPhone(OWNER_PHONE).orElseThrow().getId();
        long enrollmentId = createEnrollment();
        reset(auditLogger);

        mvc.perform(get("/api/admin/finance/event-enrollments")
                        .cookie(financeCookies)
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(enrollmentId))
                .andExpect(jsonPath("$.content[0].paymentConfirmed").value(false));

        mvc.perform(post("/api/admin/finance/event-enrollments/{id}/confirm-payment", enrollmentId)
                        .with(csrf())
                        .cookie(financeCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(enrollmentId))
                .andExpect(jsonPath("$.paymentConfirmed").value(true))
                .andExpect(jsonPath("$.paymentConfirmedAt").exists())
                .andExpect(jsonPath("$.paymentConfirmedByUserId").value(actorId));

        verify(auditLogger).fixedEventEnrollmentPaymentConfirmed(enrollmentId, actorId);

        mvc.perform(get("/api/admin/finance/summary")
                        .cookie(financeCookies)
                        .param("from", OffsetDateTime.now().plusDays(4).toString())
                        .param("to", OffsetDateTime.now().plusDays(6).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedCount").value(1))
                .andExpect(jsonPath("$.income").value(900));

        mvc.perform(post("/api/admin/finance/event-enrollments/{id}/confirm-payment", enrollmentId)
                        .with(csrf())
                        .cookie(financeCookies))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eventEnrollmentPaymentConfirmationRequiresFinanceRoleAndCsrf() throws Exception {
        Cookie[] userCookies = loginCookies(createUser(UserRole.USER).getPhone());
        Cookie[] financeCookies = loginCookies(OWNER_PHONE);
        long enrollmentId = createEnrollment();

        mvc.perform(get("/api/admin/finance/event-enrollments").cookie(userCookies))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/finance/event-enrollments/{id}/confirm-payment", enrollmentId)
                        .cookie(financeCookies))
                .andExpect(status().isForbidden());
    }

    private long createEnrollment() {
        User specialist = createUser(UserRole.SPECIALIST);
        User client = createUser(UserRole.USER);
        ServiceOffering service = new ServiceOffering();
        service.setTitleUa("Групова подія");
        service.setTitleEn("Group event");
        service.setDurationMinutes(90);
        service.setBasePrice(BigDecimal.valueOf(900));
        service.setBookingMode(ServiceBookingMode.FIXED_EVENT);
        service.setActive(true);
        service.setExternalPaymentUrl("https://pay.example.test/event");
        service = serviceOfferingRepository.save(service);

        FixedEvent event = new FixedEvent();
        event.setService(service);
        event.setSpecialist(specialist);
        event.setStartsAt(OffsetDateTime.now().plusDays(5));
        event.setEndsAt(OffsetDateTime.now().plusDays(5).plusMinutes(90));
        event.setCapacity(12);
        event.setActive(true);
        event = fixedEventRepository.save(event);

        FixedEventEnrollment enrollment = new FixedEventEnrollment();
        enrollment.setEvent(event);
        enrollment.setUser(client);
        enrollment.setStatus(FixedEventEnrollmentStatus.ACTIVE);
        enrollment.setReminderOptIn(false);
        return enrollmentRepository.save(enrollment).getId();
    }

    private User createUser(UserRole role) {
        String phone = "+38093" + SUFFIX.incrementAndGet();
        User user = new User();
        user.setPhone(phone);
        user.setFirstName("Finance");
        user.setLastName("Event");
        user.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        user.getRoles().add(UserRole.USER);
        user.getRoles().add(role);
        user.setEnabled(true);
        return userRepository.save(user);
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
