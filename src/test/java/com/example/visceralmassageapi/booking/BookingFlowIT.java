package com.example.visceralmassageapi.booking;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
import com.example.visceralmassageapi.schedule.domain.FixedEvent;
import com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus;
import com.example.visceralmassageapi.schedule.domain.SpecialistAvailabilityBlock;
import com.example.visceralmassageapi.schedule.repository.FixedEventRepository;
import com.example.visceralmassageapi.schedule.repository.SpecialistAvailabilityBlockRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingFlowIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger PHONE_SUFFIX = new AtomicInteger(5000000);
    private static final AtomicInteger AVAILABILITY_DAY = new AtomicInteger(1);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired OfficeRepository officeRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired FixedEventRepository fixedEventRepository;
    @Autowired SpecialistAvailabilityBlockRepository availabilityBlockRepository;
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
                .andExpect(jsonPath("$.serviceTitleEn").exists())
                .andExpect(jsonPath("$.officeId").value(officeId))
                .andExpect(jsonPath("$.reminderOptIn").value(true));

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2031-01-01T00:00:00Z")
                        .param("to", "2031-02-01T00:00:00Z")
                        .param("officeId", String.valueOf(officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockId))
                .andExpect(jsonPath("$[0].endsAt").value(org.hamcrest.Matchers.endsWith("T10:00:00Z")));

        mvc.perform(get("/api/bookings/my")
                        .cookie(userCookies)
                        .param("sort", "startsAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].serviceId").value(serviceId))
                .andExpect(jsonPath("$.content[0].serviceTitleEn").exists());

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
    void serviceAvailabilitySplitsLongBlockIntoBookableSlots() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] firstUserCookies = loginCookies(createUser());
        Cookie[] secondUserCookies = loginCookies(createUser());
        long officeId = createOffice();
        long serviceId = createService(true);
        long blockId = createAvailabilityWithRange(
                specialistCookies,
                officeId,
                "2031-03-02T08:00:00Z",
                "2031-03-02T12:00:00Z"
        );

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2031-03-02T00:00:00Z")
                        .param("to", "2031-03-03T00:00:00Z")
                        .param("officeId", String.valueOf(officeId))
                        .param("serviceId", String.valueOf(serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockId))
                .andExpect(jsonPath("$[0].startsAt").value("2031-03-02T08:00:00Z"))
                .andExpect(jsonPath("$[0].endsAt").value("2031-03-02T09:00:00Z"))
                .andExpect(jsonPath("$[1].id").value(blockId))
                .andExpect(jsonPath("$[1].startsAt").value("2031-03-02T09:00:00Z"))
                .andExpect(jsonPath("$[1].endsAt").value("2031-03-02T10:00:00Z"))
                .andExpect(jsonPath("$[2].startsAt").value("2031-03-02T10:00:00Z"))
                .andExpect(jsonPath("$[2].endsAt").value("2031-03-02T11:00:00Z"))
                .andExpect(jsonPath("$[3].startsAt").value("2031-03-02T11:00:00Z"))
                .andExpect(jsonPath("$[3].endsAt").value("2031-03-02T12:00:00Z"))
                .andExpect(jsonPath("$[4]").doesNotExist());

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(firstUserCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "startsAt":"2031-03-02T09:00:00Z",
                                  "reminderOptIn":false
                                }
                                """.formatted(blockId, serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsAt").value("2031-03-02T09:00:00Z"))
                .andExpect(jsonPath("$.endsAt").value("2031-03-02T10:00:00Z"));

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2031-03-02T00:00:00Z")
                        .param("to", "2031-03-03T00:00:00Z")
                        .param("officeId", String.valueOf(officeId))
                        .param("serviceId", String.valueOf(serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startsAt").value("2031-03-02T11:00:00Z"))
                .andExpect(jsonPath("$[0].endsAt").value("2031-03-02T12:00:00Z"))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(secondUserCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "startsAt":"2031-03-02T08:00:00Z",
                                  "reminderOptIn":false
                                }
                                """.formatted(blockId, serviceId)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(secondUserCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "startsAt":"2031-03-02T11:00:00Z",
                                  "reminderOptIn":false
                                }
                                """.formatted(blockId, serviceId)))
                .andExpect(status().isOk());
    }

    @Test
    void concreteAppointmentSlotIsBookableOnlyForAssignedServiceAndIsConsumedAfterBooking() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] userCookies = loginCookies(createUser());
        long officeId = createOffice();
        long serviceId = createService(true);
        long otherServiceId = createService(true);

        var slotResult = mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "officeId":%s,
                                  "status":"AVAILABLE",
                                  "itemType":"APPOINTMENT_SLOT",
                                  "serviceId":%s,
                                  "capacity":1,
                                  "startsAt":"2031-08-02T08:00:00Z",
                                  "endsAt":"2031-08-02T09:00:00Z"
                                }
                                """.formatted(officeId, serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemType").value("APPOINTMENT_SLOT"))
                .andExpect(jsonPath("$.serviceId").value(serviceId))
                .andReturn();

        long slotId = objectMapper.readTree(slotResult.getResponse().getContentAsString()).path("id").asLong();

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2031-08-02T00:00:00Z")
                        .param("to", "2031-08-03T00:00:00Z")
                        .param("officeId", String.valueOf(officeId))
                        .param("serviceId", String.valueOf(serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(slotId))
                .andExpect(jsonPath("$[0].startsAt").value("2031-08-02T08:00:00Z"))
                .andExpect(jsonPath("$[0].endsAt").value("2031-08-02T09:00:00Z"))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2031-08-02T00:00:00Z")
                        .param("to", "2031-08-03T00:00:00Z")
                        .param("officeId", String.valueOf(officeId))
                        .param("serviceId", String.valueOf(otherServiceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "startsAt":"2031-08-02T08:30:00Z",
                                  "reminderOptIn":true
                                }
                                """.formatted(slotId, serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsAt").value("2031-08-02T08:00:00Z"))
                .andExpect(jsonPath("$.endsAt").value("2031-08-02T09:00:00Z"))
                .andExpect(jsonPath("$.reminderOptIn").value(true));

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2031-08-02T00:00:00Z")
                        .param("to", "2031-08-03T00:00:00Z")
                        .param("officeId", String.valueOf(officeId))
                        .param("serviceId", String.valueOf(serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(specialistCookies)
                        .param("from", "2031-08-02T00:00:00Z")
                        .param("to", "2031-08-03T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)].booked".formatted(slotId)).value(true));

        deactivateService(serviceId);
        deactivateService(otherServiceId);
    }

    @Test
    void userCanBookFutureSlotInsideAlreadyStartedAvailabilityWindow() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] userCookies = loginCookies(createUser());
        long officeId = createOffice();
        long serviceId = createService(true);
        OffsetDateTime blockStartsAt = OffsetDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime bookingStartsAt = OffsetDateTime.now().plusHours(1).truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime blockEndsAt = bookingStartsAt.plusHours(2);
        long blockId = createAvailabilityWithRange(
                specialistCookies,
                officeId,
                blockStartsAt.toString(),
                blockEndsAt.toString()
        );

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "startsAt":"%s",
                                  "reminderOptIn":false
                }
                """.formatted(blockId, serviceId, bookingStartsAt)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startsAt").value(bookingStartsAt.toInstant().toString()));
    }

    @Test
    void blockedTimeInsideAvailabilityCutsGeneratedSlots() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] userCookies = loginCookies(createUser());
        long officeId = createOffice();
        long serviceId = createService(true);
        long blockId = createAvailabilityWithRange(
                specialistCookies,
                officeId,
                "2031-07-02T08:00:00Z",
                "2031-07-02T12:00:00Z"
        );

        mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "officeId":%s,
                                  "status":"BLOCKED",
                                  "startsAt":"2031-07-02T09:00:00Z",
                                  "endsAt":"2031-07-02T10:00:00Z"
                                }
                                """.formatted(officeId)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2031-07-02T00:00:00Z")
                        .param("to", "2031-07-03T00:00:00Z")
                        .param("officeId", String.valueOf(officeId))
                        .param("serviceId", String.valueOf(serviceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockId))
                .andExpect(jsonPath("$[0].startsAt").value("2031-07-02T08:00:00Z"))
                .andExpect(jsonPath("$[0].endsAt").value("2031-07-02T09:00:00Z"))
                .andExpect(jsonPath("$[1].startsAt").value("2031-07-02T10:00:00Z"))
                .andExpect(jsonPath("$[1].endsAt").value("2031-07-02T11:00:00Z"))
                .andExpect(jsonPath("$[2].startsAt").value("2031-07-02T11:00:00Z"))
                .andExpect(jsonPath("$[2].endsAt").value("2031-07-02T12:00:00Z"))
                .andExpect(jsonPath("$[3]").doesNotExist());

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "startsAt":"2031-07-02T09:00:00Z",
                                  "reminderOptIn":false
                                }
                                """.formatted(blockId, serviceId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicScheduleConfigExposesAppointmentBufferMinutes() throws Exception {
        mvc.perform(get("/api/schedule/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentBufferMinutes").value(30));
    }

    @Test
    void publicUnavailableIncludesComputedBuffersAroundBookingsAndEvents() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        Cookie[] userCookies = loginCookies(createUser());
        long officeId = createOffice();
        long serviceId = createService(true);
        long blockId = createAvailabilityWithRange(
                specialistCookies,
                officeId,
                "2036-01-02T10:00:00Z",
                "2036-01-02T11:00:00Z"
        );
        long bookingId = createBooking(userCookies, blockId, serviceId);

        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        ServiceOffering eventService = new ServiceOffering();
        eventService.setTitleUa("Буферна подія " + PHONE_SUFFIX.incrementAndGet());
        eventService.setDescriptionUa("Тест");
        eventService.setDurationMinutes(60);
        eventService.setBasePrice(BigDecimal.valueOf(700));
        eventService.setBookingMode(ServiceBookingMode.FIXED_EVENT);
        eventService.setActive(true);
        eventService = serviceOfferingRepository.save(eventService);

        FixedEvent event = new FixedEvent();
        event.setService(eventService);
        event.setSpecialist(specialist);
        event.setOffice(officeRepository.findById(officeId).orElseThrow());
        event.setStartsAt(OffsetDateTime.parse("2036-01-02T13:00:00Z"));
        event.setEndsAt(OffsetDateTime.parse("2036-01-02T14:00:00Z"));
        event.setCapacity(4);
        event.setActive(true);
        event = fixedEventRepository.save(event);

        mvc.perform(get("/api/schedule/unavailable")
                        .param("from", "2036-01-02T09:00:00Z")
                        .param("to", "2036-01-02T15:00:00Z")
                        .param("officeId", String.valueOf(officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'booking-buffer-%s-before' && @.status == 'BUFFER' && @.startsAt == '2036-01-02T09:30:00Z' && @.endsAt == '2036-01-02T10:00:00Z')]".formatted(bookingId)).exists())
                .andExpect(jsonPath("$[?(@.status == 'OCCUPIED' && @.startsAt == '2036-01-02T10:00:00Z' && @.endsAt == '2036-01-02T11:00:00Z')]").exists())
                .andExpect(jsonPath("$[?(@.id == 'booking-buffer-%s-after' && @.status == 'BUFFER' && @.startsAt == '2036-01-02T11:00:00Z' && @.endsAt == '2036-01-02T11:30:00Z')]".formatted(bookingId)).exists())
                .andExpect(jsonPath("$[?(@.id == 'event-%s' && @.status == 'UNAVAILABLE' && @.startsAt == '2036-01-02T13:00:00Z' && @.endsAt == '2036-01-02T14:00:00Z')]".formatted(event.getId())).exists())
                .andExpect(jsonPath("$[?(@.id == 'event-buffer-%s-before' && @.status == 'BUFFER' && @.startsAt == '2036-01-02T12:30:00Z' && @.endsAt == '2036-01-02T13:00:00Z')]".formatted(event.getId())).exists())
                .andExpect(jsonPath("$[?(@.id == 'event-buffer-%s-after' && @.status == 'BUFFER' && @.startsAt == '2036-01-02T14:00:00Z' && @.endsAt == '2036-01-02T14:30:00Z')]".formatted(event.getId())).exists());
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
        long blockId = createAvailabilityWithRange(
                specialistCookies,
                officeId,
                "2035-05-10T08:00:00Z",
                "2035-05-10T10:00:00Z"
        );
        long bookingId = createBooking(userCookies, blockId, serviceId);
        long otherOfficeId = createOffice();
        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        ServiceOffering service = serviceOfferingRepository.findById(serviceId).orElseThrow();
        service.setBasePrice(BigDecimal.valueOf(9999));
        serviceOfferingRepository.save(service);

        mvc.perform(put("/api/admin/finance/specialists/{specialistId}/settings", specialist.getId())
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"specialistSharePercent":25.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialistId").value(specialist.getId()))
                .andExpect(jsonPath("$.specialistSharePercent").value(25.0));

        mvc.perform(get("/api/admin/finance/specialists")
                        .cookie(specialistCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.specialistId == %s)]".formatted(specialist.getId())).exists())
                .andExpect(jsonPath("$[?(@.specialistId == %s)].specialistSharePercent".formatted(specialist.getId())).value(25.0));

        mvc.perform(put("/api/admin/finance/settings")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quarterlyTaxPercent":5.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quarterlyTaxPercent").value(5.0));

        mvc.perform(get("/api/admin/finance/settings")
                        .cookie(userCookies))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/specialist/finance/overview")
                        .cookie(userCookies))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/specialist/finance/overview")
                        .cookie(specialistCookies)
                        .param("from", "2035-05-01T00:00:00Z")
                        .param("to", "2035-06-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(0))
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.workedMinutes").value(0))
                .andExpect(jsonPath("$.grossIncome").value(0))
                .andExpect(jsonPath("$.specialistEarnings").value(0))
                .andExpect(jsonPath("$.pendingGrossIncome").value(1200))
                .andExpect(jsonPath("$.pendingSpecialistEarnings").value(300))
                .andExpect(jsonPath("$.specialistSharePercent").value(25.0));

        mvc.perform(get("/api/admin/finance/bookings")
                        .cookie(specialistCookies)
                        .param("status", "AWAITING_PAYMENT_CONFIRMATION")
                        .param("officeId", String.valueOf(officeId))
                        .param("from", "2035-05-01T00:00:00Z")
                        .param("to", "2035-06-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(bookingId)).exists())
                .andExpect(jsonPath("$.content[?(@.id == %s)].externalPaymentUrl".formatted(bookingId)).value("https://pay.example.com/test"))
                .andExpect(jsonPath("$.content[?(@.id == %s)].bookedPrice".formatted(bookingId)).value(1200.0))
                .andExpect(jsonPath("$.content[?(@.id == %s)].serviceTitleEn".formatted(bookingId)).exists())
                .andExpect(jsonPath("$.content[?(@.id == %s)].specialistSharePercent".formatted(bookingId)).value(25.0))
                .andExpect(jsonPath("$.content[?(@.id == %s)].specialistShare".formatted(bookingId)).value(300.0))
                .andExpect(jsonPath("$.content[?(@.id == %s)].businessShare".formatted(bookingId)).value(900.0));

        mvc.perform(get("/api/admin/finance/bookings")
                        .cookie(specialistCookies)
                        .param("officeId", String.valueOf(otherOfficeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(bookingId)).doesNotExist());

        mvc.perform(get("/api/admin/finance/bookings")
                        .cookie(specialistCookies)
                        .param("from", "2040-05-01T00:00:00Z"))
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

        mvc.perform(get("/api/specialist/finance/overview")
                        .cookie(specialistCookies)
                        .param("from", "2035-05-01T00:00:00Z")
                        .param("to", "2035-06-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedCount").value(1))
                .andExpect(jsonPath("$.pendingCount").value(0))
                .andExpect(jsonPath("$.payoutPendingCount").value(1))
                .andExpect(jsonPath("$.payoutPaidCount").value(0))
                .andExpect(jsonPath("$.workedMinutes").value(60))
                .andExpect(jsonPath("$.grossIncome").value(1200))
                .andExpect(jsonPath("$.specialistEarnings").value(300))
                .andExpect(jsonPath("$.payoutPendingEarnings").value(300))
                .andExpect(jsonPath("$.payoutPaidEarnings").value(0))
                .andExpect(jsonPath("$.pendingGrossIncome").value(0))
                .andExpect(jsonPath("$.pendingSpecialistEarnings").value(0));

        mvc.perform(get("/api/admin/finance/bookings")
                        .cookie(specialistCookies)
                        .param("status", "CONFIRMED")
                        .param("officeId", String.valueOf(officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %s)]".formatted(bookingId)).exists())
                .andExpect(jsonPath("$.content[?(@.id == %s)].specialistPayoutStatus".formatted(bookingId)).value("PENDING"));

        mvc.perform(post("/api/admin/finance/bookings/{id}/specialist-payout/mark-paid", bookingId)
                        .with(csrf())
                        .cookie(userCookies))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/finance/bookings/{id}/specialist-payout/mark-paid", bookingId)
                        .with(csrf())
                        .cookie(specialistCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialistPayoutStatus").value("PAID"))
                .andExpect(jsonPath("$.specialistPayoutPaidAt").exists())
                .andExpect(jsonPath("$.specialistPayoutPaidByUserId").value(specialist.getId()));

        mvc.perform(post("/api/admin/finance/bookings/{id}/specialist-payout/mark-paid", bookingId)
                        .with(csrf())
                        .cookie(specialistCookies))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/specialist/finance/overview")
                        .cookie(specialistCookies)
                        .param("from", "2035-05-01T00:00:00Z")
                        .param("to", "2035-06-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payoutPendingCount").value(0))
                .andExpect(jsonPath("$.payoutPaidCount").value(1))
                .andExpect(jsonPath("$.payoutPendingEarnings").value(0))
                .andExpect(jsonPath("$.payoutPaidEarnings").value(300));

        mvc.perform(get("/api/admin/finance/export/xlsx")
                        .cookie(specialistCookies)
                        .param("status", "CONFIRMED")
                        .param("officeId", String.valueOf(officeId))
                        .param("from", "2035-05-01T00:00:00Z")
                        .param("to", "2035-06-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("ataraksia-finance.xlsx")));

        mvc.perform(get("/api/admin/finance/export/pdf")
                        .cookie(specialistCookies)
                        .param("status", "CONFIRMED")
                        .param("officeId", String.valueOf(officeId))
                        .param("from", "2035-05-01T00:00:00Z")
                        .param("to", "2035-06-01T00:00:00Z")
                        .param("locale", "en"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/pdf")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("ataraksia-finance.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Booking ")));

        mvc.perform(post("/api/admin/finance/expenses")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount":250,
                                  "category":"Materials",
                                  "description":"Booking flow expense",
                                  "expenseDate":"2035-05-10",
                                  "officeId":%s
                                }
                                """.formatted(officeId)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/finance/summary")
                        .cookie(specialistCookies)
                        .param("officeId", String.valueOf(officeId))
                        .param("from", "2035-05-01T00:00:00Z")
                        .param("to", "2035-06-01T00:00:00Z")
                        .param("expenseFrom", "2035-05-01")
                        .param("expenseTo", "2035-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").value(0))
                .andExpect(jsonPath("$.confirmedCount").value(1))
                .andExpect(jsonPath("$.income").value(1200))
                .andExpect(jsonPath("$.specialistEarnings").value(300))
                .andExpect(jsonPath("$.businessIncome").value(900))
                .andExpect(jsonPath("$.expenses").value(250))
                .andExpect(jsonPath("$.taxableIncome").value(650))
                .andExpect(jsonPath("$.quarterlyTaxPercent").value(5.0))
                .andExpect(jsonPath("$.estimatedTax").value(32.5))
                .andExpect(jsonPath("$.result").value(650));

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
        long blockId = createAvailabilityWithRange(
                specialistCookies,
                officeId,
                "2031-06-02T08:00:00Z",
                "2031-06-02T10:00:00Z"
        );
        long bookingId = createBooking(userCookies, blockId, serviceId);

        mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "officeId":%s,
                                  "status":"BLOCKED",
                                  "startsAt":"2031-06-02T08:30:00Z",
                                  "endsAt":"2031-06-02T09:30:00Z"
                                }
                                """.formatted(officeId)))
                .andExpect(status().isBadRequest());

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
                        .param("from", "2031-06-02T00:00:00Z")
                        .param("to", "2031-06-03T00:00:00Z")
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

    @Test
    void userCannotCancelBookingAfterItStarted() throws Exception {
        String userPhone = createUser();
        Cookie[] userCookies = loginCookies(userPhone);
        long officeId = createOffice();
        long serviceId = createService(true);
        User user = userRepository.findByPhone(userPhone).orElseThrow();
        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        Office office = officeRepository.findById(officeId).orElseThrow();
        ServiceOffering service = serviceOfferingRepository.findById(serviceId).orElseThrow();
        SpecialistAvailabilityBlock block = new SpecialistAvailabilityBlock();
        block.setSpecialist(specialist);
        block.setOffice(office);
        block.setStatus(ScheduleBlockStatus.AVAILABLE);
        block.setStartsAt(OffsetDateTime.parse("2025-05-02T08:00:00Z"));
        block.setEndsAt(OffsetDateTime.parse("2025-05-02T10:00:00Z"));
        block = availabilityBlockRepository.save(block);
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setSpecialist(specialist);
        booking.setService(service);
        booking.setOffice(office);
        booking.setAvailabilityBlock(block);
        booking.setStatus(BookingStatus.AWAITING_PAYMENT_CONFIRMATION);
        booking.setStartsAt(OffsetDateTime.parse("2025-05-02T08:00:00Z"));
        booking.setEndsAt(OffsetDateTime.parse("2025-05-02T09:00:00Z"));
        booking.setBookedPrice(service.getBasePrice());
        booking.setReminderOptIn(false);
        booking = bookingRepository.save(booking);

        mvc.perform(post("/api/bookings/{id}/cancel", booking.getId())
                        .with(csrf())
                        .cookie(userCookies))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fixedEventEnrollmentKeepsEventVisibleAndEnforcesCapacity() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        String firstUserPhone = createUser();
        String secondUserPhone = createUser();
        Cookie[] firstUserCookies = loginCookies(firstUserPhone);
        Cookie[] secondUserCookies = loginCookies(secondUserPhone);
        long officeId = createOffice();
        long individualServiceId = createService(true);
        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        ServiceOffering eventService = new ServiceOffering();
        eventService.setTitleUa("Тестова групова подія " + PHONE_SUFFIX.incrementAndGet());
        eventService.setDescriptionUa("Тест");
        eventService.setDurationMinutes(90);
        eventService.setBasePrice(BigDecimal.valueOf(700));
        eventService.setBookingMode(ServiceBookingMode.FIXED_EVENT);
        eventService.setActive(true);
        eventService = serviceOfferingRepository.save(eventService);
        FixedEvent event = new FixedEvent();
        event.setService(eventService);
        event.setSpecialist(specialist);
        event.setOffice(officeRepository.findById(officeId).orElseThrow());
        event.setStartsAt(java.time.OffsetDateTime.parse("2031-04-02T09:00:00Z"));
        event.setEndsAt(java.time.OffsetDateTime.parse("2031-04-02T10:00:00Z"));
        event.setCapacity(1);
        event.setNote("Public test event");
        event.setActive(true);
        event = fixedEventRepository.save(event);
        long availabilityId = createAvailabilityWithRange(
                specialistCookies,
                officeId,
                "2031-04-02T08:00:00Z",
                "2031-04-02T12:00:00Z"
        );

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2031-04-02T00:00:00Z")
                        .param("to", "2031-04-03T00:00:00Z")
                        .param("officeId", String.valueOf(officeId))
                        .param("serviceId", String.valueOf(individualServiceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(availabilityId))
                .andExpect(jsonPath("$[0].startsAt").value("2031-04-02T11:00:00Z"))
                .andExpect(jsonPath("$[0].endsAt").value("2031-04-02T12:00:00Z"))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mvc.perform(get("/api/schedule/unavailable")
                        .param("from", "2031-04-02T00:00:00Z")
                        .param("to", "2031-04-03T00:00:00Z")
                        .param("officeId", String.valueOf(officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 'event-%s')]".formatted(event.getId())).exists())
                .andExpect(jsonPath("$[?(@.id == 'event-%s')].status".formatted(event.getId())).value("UNAVAILABLE"));

        mvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(firstUserCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "startsAt":"2031-04-02T08:00:00Z",
                                  "reminderOptIn":false
                                }
                                """.formatted(availabilityId, individualServiceId)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "officeId":%s,
                                  "status":"BLOCKED",
                                  "startsAt":"2031-04-02T09:30:00Z",
                                  "endsAt":"2031-04-02T10:30:00Z"
                                }
                                """.formatted(officeId)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/schedule/events")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceId":%s,
                                  "officeId":%s,
                                  "startsAt":"2031-04-02T09:30:00Z",
                                  "endsAt":"2031-04-02T10:30:00Z",
                                  "capacity":4,
                                  "active":true
                                }
                                """.formatted(eventService.getId(), officeId)))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/schedule/events")
                        .param("from", "2031-04-01T00:00:00Z")
                        .param("to", "2031-04-08T00:00:00Z")
                        .param("serviceId", String.valueOf(eventService.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(event.getId()))
                .andExpect(jsonPath("$[0].remainingPlaces").value(1))
                .andExpect(jsonPath("$[0].enrolled").value(false));

        mvc.perform(post("/api/schedule/events/{id}/enroll", event.getId())
                        .with(csrf())
                        .cookie(firstUserCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reminderOptIn\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true))
                .andExpect(jsonPath("$.remainingPlaces").value(0))
                .andExpect(jsonPath("$.full").value(true));

        mvc.perform(get("/api/admin/schedule/events/enrollments")
                        .cookie(specialistCookies)
                        .param("from", "2031-04-01T00:00:00Z")
                        .param("to", "2031-04-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(event.getId()))
                .andExpect(jsonPath("$[0].eventTitle").value(eventService.getTitleUa()))
                .andExpect(jsonPath("$[0].clientName").value("Booking Client"))
                .andExpect(jsonPath("$[0].clientContact").value(firstUserPhone))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].reminderOptIn").value(true));

        mvc.perform(get("/api/schedule/events")
                        .cookie(firstUserCookies)
                        .param("from", "2031-04-01T00:00:00Z")
                        .param("to", "2031-04-08T00:00:00Z")
                        .param("serviceId", String.valueOf(eventService.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(event.getId()))
                .andExpect(jsonPath("$[0].enrolled").value(true));

        mvc.perform(get("/api/schedule/events/my")
                        .cookie(firstUserCookies)
                        .param("from", "2031-04-01T00:00:00Z")
                        .param("to", "2031-04-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(event.getId()))
                .andExpect(jsonPath("$[0].enrolled").value(true))
                .andExpect(jsonPath("$[0].remainingPlaces").value(0));

        mvc.perform(post("/api/schedule/events/{id}/enroll", event.getId())
                        .with(csrf())
                        .cookie(secondUserCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reminderOptIn\":false}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/schedule/events/{id}/cancel", event.getId())
                        .with(csrf())
                        .cookie(firstUserCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(false))
                .andExpect(jsonPath("$.remainingPlaces").value(1));

        mvc.perform(get("/api/schedule/events/my")
                        .cookie(firstUserCookies)
                        .param("from", "2031-04-01T00:00:00Z")
                        .param("to", "2031-04-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());

        mvc.perform(post("/api/schedule/events/{id}/enroll", event.getId())
                        .with(csrf())
                        .cookie(secondUserCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reminderOptIn\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true))
                .andExpect(jsonPath("$.remainingPlaces").value(0));
    }

    @Test
    void specialistCanDeactivatePastFixedEventWithoutMovingItsTime() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        long officeId = createOffice();
        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        ServiceOffering eventService = new ServiceOffering();
        eventService.setTitleUa("Минула подія " + PHONE_SUFFIX.incrementAndGet());
        eventService.setDescriptionUa("Тест");
        eventService.setDurationMinutes(90);
        eventService.setBasePrice(BigDecimal.valueOf(700));
        eventService.setBookingMode(ServiceBookingMode.FIXED_EVENT);
        eventService.setActive(true);
        eventService = serviceOfferingRepository.save(eventService);
        FixedEvent event = new FixedEvent();
        event.setService(eventService);
        event.setSpecialist(specialist);
        event.setOffice(officeRepository.findById(officeId).orElseThrow());
        event.setStartsAt(java.time.OffsetDateTime.parse("2025-04-02T08:00:00Z"));
        event.setEndsAt(java.time.OffsetDateTime.parse("2025-04-02T09:30:00Z"));
        event.setCapacity(4);
        event.setNote("Past event");
        event.setActive(true);
        event = fixedEventRepository.save(event);

        mvc.perform(put("/api/admin/schedule/events/{id}", event.getId())
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceId":%s,
                                  "officeId":%s,
                                  "startsAt":"2025-04-02T08:00:00Z",
                                  "endsAt":"2025-04-02T09:30:00Z",
                                  "capacity":4,
                                  "note":"Past event hidden",
                                  "active":false
                                }
                                """.formatted(eventService.getId(), officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.note").value("Past event hidden"));
    }

    private long createAvailability(Cookie[] cookies, long officeId) throws Exception {
        int day = AVAILABILITY_DAY.getAndIncrement();
        String startsAt = "2031-01-%02dT08:00:00Z".formatted(day);
        String endsAt = "2031-01-%02dT10:00:00Z".formatted(day);

        return createAvailabilityWithRange(cookies, officeId, startsAt, endsAt);
    }

    private long createAvailabilityWithRange(Cookie[] cookies, long officeId, String startsAt, String endsAt) throws Exception {
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
        int suffix = PHONE_SUFFIX.incrementAndGet();
        service.setTitleUa("Бронювання " + suffix);
        service.setTitleEn("Booking " + suffix);
        service.setDescriptionUa("Тестова послуга");
        service.setDescriptionEn("Test service");
        service.setDurationMinutes(60);
        service.setBasePrice(BigDecimal.valueOf(1200));
        service.setActive(active);
        service.setExternalPaymentUrl("https://pay.example.com/test");
        return serviceOfferingRepository.save(service).getId();
    }

    private void deactivateService(long serviceId) {
        ServiceOffering service = serviceOfferingRepository.findById(serviceId).orElseThrow();
        service.setActive(false);
        serviceOfferingRepository.save(service);
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
