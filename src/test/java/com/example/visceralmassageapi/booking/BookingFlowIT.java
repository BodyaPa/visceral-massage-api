package com.example.visceralmassageapi.booking;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingFlowIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger PHONE_SUFFIX = new AtomicInteger(5000000);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired OfficeRepository officeRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void ownerBootstrapProperties(DynamicPropertyRegistry registry) {
        registry.add("app.owner.bootstrap.enabled", () -> "true");
        registry.add("app.owner.bootstrap.phone", () -> OWNER_PHONE);
        registry.add("app.owner.bootstrap.email", () -> "OWNER@EXAMPLE.COM");
        registry.add("app.owner.bootstrap.password", () -> OWNER_PASSWORD);
    }

    @Test
    void authenticatedUserCanBookAvailableBlockOnce() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] userCookies = loginCookies(createUser());
        long officeId = createOffice();
        long serviceId = createService(true);
        long blockId = createAvailability(specialistCookies, officeId);

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "reminderOptIn":true
                                }
                                """.formatted(blockId, serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT_CONFIRMATION"))
                .andExpect(jsonPath("$.serviceId").value(serviceId))
                .andExpect(jsonPath("$.officeId").value(officeId))
                .andExpect(jsonPath("$.reminderOptIn").value(true));

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2031-01-01T00:00:00Z")
                        .param("to", "2031-02-01T00:00:00Z")
                        .param("officeId", String.valueOf(officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)]".formatted(blockId)).doesNotExist());

        mvc.perform(get("/api/bookings/my")
                        .cookie(userCookies)
                        .param("sort", "startsAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].serviceId").value(serviceId));

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "reminderOptIn":false
                                }
                                """.formatted(blockId, serviceId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookingRequiresCsrfAndActiveService() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] userCookies = loginCookies(createUser());
        long officeId = createOffice();
        long inactiveServiceId = createService(false);
        long blockId = createAvailability(specialistCookies, officeId);

        mvc.perform(post("/api/bookings")
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availabilityBlockId":%s,"serviceId":%s}
                                """.formatted(blockId, inactiveServiceId)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availabilityBlockId":%s,"serviceId":%s}
                                """.formatted(blockId, inactiveServiceId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentBookingRequestsCreateOnlyOneActiveBooking() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] firstUserCookies = loginCookies(createUser());
        Cookie[] secondUserCookies = loginCookies(createUser());
        long officeId = createOffice();
        long serviceId = createService(true);
        long blockId = createAvailability(specialistCookies, officeId);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = List.of(
                    executor.submit(() -> concurrentBookingStatus(firstUserCookies, blockId, serviceId, ready, start)),
                    executor.submit(() -> concurrentBookingStatus(secondUserCookies, blockId, serviceId, ready, start))
            );

            ready.await();
            start.countDown();

            List<Integer> statuses = futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
            }).sorted().toList();

            org.assertj.core.api.Assertions.assertThat(statuses).containsExactly(200, 400);
        }

        org.assertj.core.api.Assertions.assertThat(
                bookingRepository.countByAvailabilityBlockIdAndStatusNot(blockId, BookingStatus.CANCELLED)
        ).isEqualTo(1);
    }

    @Test
    void financeManagerCanListAndConfirmPayment() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] userCookies = loginCookies(createUser());
        long officeId = createOffice();
        long serviceId = createService(true);
        long blockId = createAvailability(specialistCookies, officeId);
        long bookingId = createBooking(userCookies, blockId, serviceId);
        long otherOfficeId = createOffice();
        ServiceOffering service = serviceOfferingRepository.findById(serviceId).orElseThrow();
        service.setBasePrice(BigDecimal.valueOf(9999));
        serviceOfferingRepository.save(service);

        mvc.perform(get("/api/admin/finance/bookings")
                        .cookie(specialistCookies)
                        .param("status", "AWAITING_PAYMENT_CONFIRMATION")
                        .param("officeId", String.valueOf(officeId))
                        .param("from", "2031-01-01T00:00:00Z")
                        .param("to", "2031-02-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(bookingId)).exists())
                .andExpect(jsonPath("$.content[?(@.id == %s)].basePrice".formatted(bookingId)).value(1200.0));

        mvc.perform(get("/api/admin/finance/bookings")
                        .cookie(specialistCookies)
                        .param("officeId", String.valueOf(otherOfficeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(bookingId)).doesNotExist());

        mvc.perform(get("/api/admin/finance/bookings")
                        .cookie(specialistCookies)
                        .param("from", "2040-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(bookingId)).doesNotExist());

        mvc.perform(post("/api/admin/finance/bookings/{id}/confirm-payment", bookingId)
                        .with(csrf())
                        .cookie(userCookies))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/finance/bookings/{id}/confirm-payment", bookingId)
                        .with(csrf())
                        .cookie(specialistCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mvc.perform(get("/api/admin/finance/bookings")
                        .cookie(specialistCookies)
                        .param("status", "CONFIRMED")
                        .param("officeId", String.valueOf(officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(bookingId)).exists());

        mvc.perform(post("/api/admin/finance/expenses")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount":250,
                                  "category":"Materials",
                                  "description":"Booking flow expense",
                                  "expenseDate":"2031-01-10",
                                  "officeId":%s
                                }
                                """.formatted(officeId)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/finance/summary")
                        .cookie(specialistCookies)
                        .param("officeId", String.valueOf(officeId))
                        .param("from", "2031-01-01T00:00:00Z")
                        .param("to", "2031-02-01T00:00:00Z")
                        .param("expenseFrom", "2031-01-01")
                        .param("expenseTo", "2031-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").value(0))
                .andExpect(jsonPath("$.confirmedCount").value(1))
                .andExpect(jsonPath("$.income").value(1200))
                .andExpect(jsonPath("$.expenses").value(250))
                .andExpect(jsonPath("$.result").value(950));

        mvc.perform(get("/api/admin/finance/summary")
                        .cookie(userCookies)
                        .param("officeId", String.valueOf(officeId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void financeBookingFilterRejectsInvalidDateRange() throws Exception {
        Cookie[] financeCookies = loginCookies(OWNER_PHONE);

        mvc.perform(get("/api/admin/finance/bookings")
                        .cookie(financeCookies)
                        .param("from", "2031-02-01T00:00:00Z")
                        .param("to", "2031-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/admin/finance/summary")
                        .cookie(financeCookies)
                        .param("expenseFrom", "2031-02-01")
                        .param("expenseTo", "2031-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void specialistCanListOnlyOwnBookingsInRange() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] userCookies = loginCookies(createUser());
        long officeId = createOffice();
        long serviceId = createService(true);
        long blockId = createAvailability(specialistCookies, officeId);
        long bookingId = createBooking(userCookies, blockId, serviceId);

        mvc.perform(get("/api/admin/schedule/bookings")
                        .cookie(specialistCookies)
                        .param("from", "2031-01-01T00:00:00Z")
                        .param("to", "2031-02-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)]".formatted(bookingId)).exists())
                .andExpect(jsonPath("$[?(@.id == %s)].clientName".formatted(bookingId)).isNotEmpty());

        mvc.perform(get("/api/admin/schedule/bookings")
                        .cookie(userCookies)
                        .param("from", "2031-01-01T00:00:00Z")
                        .param("to", "2031-02-01T00:00:00Z"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanCancelOwnBookingAndReleasePublicSlot() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] userCookies = loginCookies(createUser());
        Cookie[] otherUserCookies = loginCookies(createUser());
        long officeId = createOffice();
        long serviceId = createService(true);
        long blockId = createAvailability(specialistCookies, officeId);
        long bookingId = createBooking(userCookies, blockId, serviceId);

        mvc.perform(post("/api/bookings/{id}/cancel", bookingId)
                        .with(csrf())
                        .cookie(otherUserCookies))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/bookings/{id}/cancel", bookingId)
                        .with(csrf())
                        .cookie(userCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2031-01-01T00:00:00Z")
                        .param("to", "2031-02-01T00:00:00Z")
                        .param("officeId", String.valueOf(officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)]".formatted(blockId)).exists());

        mvc.perform(post("/api/bookings/{id}/cancel", bookingId)
                        .with(csrf())
                        .cookie(userCookies))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(otherUserCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "reminderOptIn":false
                                }
                                """.formatted(blockId, serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(bookingId)))
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT_CONFIRMATION"));
    }

    private long createAvailability(Cookie[] cookies, long officeId) throws Exception {
        int day = 1 + Math.floorMod(PHONE_SUFFIX.incrementAndGet(), 20);
        String startsAt = "2031-01-%02dT08:00:00Z".formatted(day);
        String endsAt = "2031-01-%02dT10:00:00Z".formatted(day);

        var result = mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "officeId":%s,
                                  "status":"AVAILABLE",
                                  "startsAt":"%s",
                                  "endsAt":"%s"
                                }
                                """.formatted(officeId, startsAt, endsAt)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private long createBooking(Cookie[] userCookies, long blockId, long serviceId) throws Exception {
        var result = mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "reminderOptIn":false
                                }
                                """.formatted(blockId, serviceId)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private int concurrentBookingStatus(
            Cookie[] userCookies,
            long blockId,
            long serviceId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await();

        return mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "reminderOptIn":false
                                }
                                """.formatted(blockId, serviceId)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private long createOffice() {
        Office office = new Office();
        office.setName("Booking Office " + PHONE_SUFFIX.incrementAndGet());
        office.setAddress("Kyiv");
        office.setActive(true);
        return officeRepository.save(office).getId();
    }

    private long createService(boolean active) {
        ServiceOffering service = new ServiceOffering();
        service.setTitleUa("Бронювання " + PHONE_SUFFIX.incrementAndGet());
        service.setDescriptionUa("Тестова послуга");
        service.setDurationMinutes(60);
        service.setBasePrice(BigDecimal.valueOf(1200));
        service.setActive(active);
        service.setExternalPaymentUrl("https://pay.example.com/test");
        return serviceOfferingRepository.save(service).getId();
    }

    private String createUser() {
        String phone = uniquePhone();
        User user = new User();
        user.setPhone(phone);
        user.setFirstName("Booking");
        user.setLastName("Client");
        user.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        user.getRoles().add(UserRole.USER);
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

    private String uniquePhone() {
        return "+38095" + PHONE_SUFFIX.incrementAndGet();
    }
}
