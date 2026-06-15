package com.example.visceralmassageapi.schedule;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.schedule.domain.SpecialistAvailabilityBlock;
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

import java.util.concurrent.atomic.AtomicInteger;
import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SpecialistScheduleManagementIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";
    private static final AtomicInteger PHONE_SUFFIX = new AtomicInteger(4000000);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired OfficeRepository officeRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired BookingRepository bookingRepository;
    @Autowired SpecialistAvailabilityBlockRepository availabilityBlockRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;

    @DynamicPropertySource
    static void ownerBootstrapProperties(DynamicPropertyRegistry registry) {
        registry.add("app.owner.bootstrap.enabled", () -> "true");
        registry.add("app.owner.bootstrap.phone", () -> OWNER_PHONE);
        registry.add("app.owner.bootstrap.email", () -> "OWNER@EXAMPLE.COM");
        registry.add("app.owner.bootstrap.password", () -> OWNER_PASSWORD);
    }

    @Test
    void specialistCanCreateListAndDeleteOwnAvailability() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        long officeId = createOffice();

        var createResult = mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "officeId":%s,
                                  "status":"AVAILABLE",
                                  "startsAt":"2030-01-02T08:00:00Z",
                                  "endsAt":"2030-01-02T12:00:00Z",
                                  "notes":"  Morning slots  "
                                }
                                """.formatted(officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.officeId").value(officeId))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.notes").value("Morning slots"))
                .andReturn();

        long blockId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(specialistCookies)
                        .param("from", "2030-01-01T00:00:00Z")
                        .param("to", "2030-01-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockId));

        mvc.perform(delete("/api/admin/schedule/availability/{id}", blockId)
                        .with(csrf())
                        .cookie(specialistCookies))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(specialistCookies)
                        .param("from", "2030-01-01T00:00:00Z")
                        .param("to", "2030-01-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void specialistAvailabilityAllowsBlockedExceptionsButRejectsSameTypeOverlap() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);

        mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"AVAILABLE",
                                  "startsAt":"2030-02-02T08:00:00Z",
                                  "endsAt":"2030-02-02T12:00:00Z"
                                }
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"BLOCKED",
                                  "startsAt":"2030-02-02T11:00:00Z",
                                  "endsAt":"2030-02-02T13:00:00Z"
                                }
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"AVAILABLE",
                                  "startsAt":"2030-02-02T10:00:00Z",
                                  "endsAt":"2030-02-02T14:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void specialistCanCreateConcreteAppointmentSlotForSpecificService() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        long officeId = createOffice();
        ServiceOffering service = createService();
        ServiceOffering otherService = createService();

        var createResult = mvc.perform(post("/api/admin/schedule/availability")
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
                                  "startsAt":"2030-02-03T08:00:00Z",
                                  "endsAt":"2030-02-03T09:00:00Z",
                                  "notes":"Concrete massage slot"
                                }
                                """.formatted(officeId, service.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemType").value("APPOINTMENT_SLOT"))
                .andExpect(jsonPath("$.serviceId").value(service.getId()))
                .andExpect(jsonPath("$.capacity").value(1))
                .andReturn();

        long blockId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2030-02-03T00:00:00Z")
                        .param("to", "2030-02-04T00:00:00Z")
                        .param("serviceId", String.valueOf(service.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockId))
                .andExpect(jsonPath("$[0].startsAt").value("2030-02-03T08:00:00Z"))
                .andExpect(jsonPath("$[0].endsAt").value("2030-02-03T09:00:00Z"));

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2030-02-03T00:00:00Z")
                        .param("to", "2030-02-04T00:00:00Z")
                        .param("serviceId", String.valueOf(otherService.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void masterSpecialistCanManageOtherSpecialistScheduleButRegularSpecialistCannot() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);
        User otherSpecialist = createUserWithRoles(uniquePhone(), UserRole.SPECIALIST);
        Cookie[] otherCookies = loginCookies(otherSpecialist.getPhone());

        var createResult = mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specialistId":%s,
                                  "status":"AVAILABLE",
                                  "startsAt":"2030-02-04T08:00:00Z",
                                  "endsAt":"2030-02-04T10:00:00Z"
                                }
                                """.formatted(otherSpecialist.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialistId").value(otherSpecialist.getId()))
                .andReturn();

        long blockId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(ownerCookies)
                        .param("from", "2030-02-04T00:00:00Z")
                        .param("to", "2030-02-05T00:00:00Z")
                        .param("specialistId", String.valueOf(otherSpecialist.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockId));

        mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(otherCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specialistId":%s,
                                  "status":"AVAILABLE",
                                  "startsAt":"2030-02-04T11:00:00Z",
                                  "endsAt":"2030-02-04T12:00:00Z"
                                }
                                """.formatted(userRepository.findByPhone(OWNER_PHONE).orElseThrow().getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void specialistCanUpdateOwnAvailabilityAndOverlapValidationExcludesCurrentBlock() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        long officeId = createOffice();
        long otherOfficeId = createOffice();

        var createResult = mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "officeId":%s,
                                  "status":"AVAILABLE",
                                  "startsAt":"2030-02-05T08:00:00Z",
                                  "endsAt":"2030-02-05T12:00:00Z",
                                  "notes":"Original"
                                }
                                """.formatted(officeId)))
                .andExpect(status().isOk())
                .andReturn();

        long blockId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mvc.perform(put("/api/admin/schedule/availability/{id}", blockId)
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "officeId":%s,
                                  "status":"AVAILABLE",
                                  "startsAt":"2030-02-05T09:00:00Z",
                                  "endsAt":"2030-02-05T13:00:00Z",
                                  "notes":"  Updated block  "
                                }
                                """.formatted(otherOfficeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(blockId))
                .andExpect(jsonPath("$.officeId").value(otherOfficeId))
                .andExpect(jsonPath("$.startsAt").value("2030-02-05T09:00:00Z"))
                .andExpect(jsonPath("$.endsAt").value("2030-02-05T13:00:00Z"))
                .andExpect(jsonPath("$.notes").value("Updated block"));
    }

    @Test
    void specialistCannotUpdateAvailabilityRangeWithBookingHistoryButCanUpdateNotes() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        User client = createUserWithRoles(uniquePhone());
        SpecialistAvailabilityBlock block = createAvailabilityEntity(specialist);
        Booking booking = new Booking();
        booking.setUser(client);
        booking.setSpecialist(specialist);
        ServiceOffering service = createService();
        booking.setService(service);
        booking.setAvailabilityBlock(block);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStartsAt(block.getStartsAt());
        booking.setEndsAt(block.getEndsAt());
        booking.setBookedPrice(service.getBasePrice());
        bookingRepository.save(booking);

        mvc.perform(put("/api/admin/schedule/availability/{id}", block.getId())
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"AVAILABLE",
                                  "startsAt":"2032-01-02T08:30:00Z",
                                  "endsAt":"2032-01-02T09:30:00Z",
                                  "notes":"Moved"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/admin/schedule/availability/{id}", block.getId())
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"AVAILABLE",
                                  "startsAt":"2032-01-02T08:00:00Z",
                                  "endsAt":"2032-01-02T09:00:00Z",
                                  "notes":"  Notes only  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booked").value(true))
                .andExpect(jsonPath("$.notes").value("Notes only"));
    }

    @Test
    void publicScheduleReturnsOnlyAvailableBlocksAndSupportsOfficeFilter() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        long officeId = createOffice();
        long otherOfficeId = createOffice();

        createAvailability(specialistCookies, officeId, "AVAILABLE", "2030-04-02T08:00:00Z", "2030-04-02T10:00:00Z");
        createAvailability(specialistCookies, otherOfficeId, "AVAILABLE", "2030-04-02T10:00:00Z", "2030-04-02T12:00:00Z");
        createAvailability(specialistCookies, officeId, "BLOCKED", "2030-04-02T12:00:00Z", "2030-04-02T14:00:00Z");

        mvc.perform(get("/api/schedule/availability")
                        .param("from", "2030-04-01T00:00:00Z")
                        .param("to", "2030-04-08T00:00:00Z")
                        .param("officeId", String.valueOf(officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].officeId").value(officeId))
                .andExpect(jsonPath("$[0].specialistName").isNotEmpty())
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void scheduleMutationsRequireCsrfAndSpecialistRole() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);

        mvc.perform(post("/api/admin/schedule/availability")
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"AVAILABLE",
                                  "startsAt":"2030-03-02T08:00:00Z",
                                  "endsAt":"2030-03-02T12:00:00Z"
                                }
                                """))
                .andExpect(status().isForbidden());

        String financeOnlyPhone = uniquePhone();
        createUserWithRoles(financeOnlyPhone, UserRole.FINANCE_MANAGER);
        Cookie[] financeOnlyCookies = loginCookies(financeOnlyPhone);

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(financeOnlyCookies)
                        .param("from", "2030-03-01T00:00:00Z")
                        .param("to", "2030-03-08T00:00:00Z"))
                .andExpect(status().isForbidden());
    }

    @Test
    void specialistCanCreateManualBookingOnlyForOwnAvailableBlock() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        User client = createUserWithRoles(uniquePhone());
        SpecialistAvailabilityBlock ownBlock = createAvailabilityEntity(specialist);
        ServiceOffering service = createService();

        mvc.perform(post("/api/admin/schedule/bookings")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientIdentifier":"%s",
                                  "availabilityBlockId":%s,
                                  "serviceId":%s,
                                  "reminderOptIn":true
                                }
                                """.formatted(client.getPhone(), ownBlock.getId(), service.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(client.getId()))
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT_CONFIRMATION"))
                .andExpect(jsonPath("$.reminderOptIn").value(true));

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(specialistCookies)
                        .param("from", "2032-01-01T00:00:00Z")
                        .param("to", "2032-01-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)].booked".formatted(ownBlock.getId())).value(true));

        mvc.perform(post("/api/admin/schedule/bookings")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientIdentifier":"%s",
                                  "availabilityBlockId":%s,
                                  "serviceId":%s
                                }
                                """.formatted(client.getPhone(), ownBlock.getId(), service.getId())))
                .andExpect(status().isBadRequest());

        User otherSpecialist = createUserWithRoles(uniquePhone(), UserRole.SPECIALIST);
        SpecialistAvailabilityBlock otherBlock = createAvailabilityEntity(otherSpecialist);

        mvc.perform(post("/api/admin/schedule/bookings")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientIdentifier":"%s",
                                  "availabilityBlockId":%s,
                                  "serviceId":%s
                                }
                                """.formatted(client.getPhone(), otherBlock.getId(), service.getId())))
                .andExpect(status().isNotFound());

        Cookie[] clientCookies = loginCookies(client.getPhone());
        mvc.perform(post("/api/admin/schedule/bookings")
                        .with(csrf())
                        .cookie(clientCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientIdentifier":"%s",
                                  "availabilityBlockId":%s,
                                  "serviceId":%s
                                }
                                """.formatted(client.getPhone(), ownBlock.getId(), service.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void masterSpecialistCanListOtherSpecialistBookingsButRegularSpecialistCannot() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);
        User otherSpecialist = createUserWithRoles(uniquePhone(), UserRole.SPECIALIST);
        User regularSpecialist = createUserWithRoles(uniquePhone(), UserRole.SPECIALIST);
        Cookie[] regularSpecialistCookies = loginCookies(regularSpecialist.getPhone());
        User client = createUserWithRoles(uniquePhone());
        SpecialistAvailabilityBlock block = createAvailabilityEntity(
                otherSpecialist,
                "2032-03-02T08:00:00Z",
                "2032-03-02T09:00:00Z"
        );
        ServiceOffering service = createService();
        Booking booking = createBookingEntity(client, otherSpecialist, block, service);

        mvc.perform(get("/api/admin/schedule/bookings")
                        .cookie(ownerCookies)
                        .param("specialistId", String.valueOf(otherSpecialist.getId()))
                        .param("from", "2032-03-01T00:00:00Z")
                        .param("to", "2032-03-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(booking.getId()))
                .andExpect(jsonPath("$[0].clientId").value(client.getId()))
                .andExpect(jsonPath("$[0].specialistId").value(otherSpecialist.getId()));

        mvc.perform(get("/api/admin/schedule/bookings")
                        .cookie(regularSpecialistCookies)
                        .param("specialistId", String.valueOf(otherSpecialist.getId()))
                        .param("from", "2032-03-01T00:00:00Z")
                        .param("to", "2032-03-08T00:00:00Z"))
                .andExpect(status().isForbidden());
    }

    @Test
    void masterSpecialistDefaultScheduleListsAllSpecialistsCalendarItems() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);
        User owner = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        User otherSpecialist = createUserWithRoles(uniquePhone(), UserRole.SPECIALIST);
        User client = createUserWithRoles(uniquePhone());
        ServiceOffering bookingService = createService();
        SpecialistAvailabilityBlock ownerBlock = createAvailabilityEntity(
                owner,
                "2032-04-02T08:00:00Z",
                "2032-04-02T09:00:00Z"
        );
        SpecialistAvailabilityBlock otherBlock = createAvailabilityEntity(
                otherSpecialist,
                "2032-04-02T10:00:00Z",
                "2032-04-02T11:00:00Z"
        );
        Booking booking = createBookingEntity(client, otherSpecialist, otherBlock, bookingService);
        SpecialistAvailabilityBlock cancelledBlock = createAvailabilityEntity(
                otherSpecialist,
                "2032-04-02T12:00:00Z",
                "2032-04-02T13:00:00Z"
        );
        Booking cancelledBooking = createBookingEntity(client, otherSpecialist, cancelledBlock, bookingService);
        cancelledBooking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(cancelledBooking);
        ServiceOffering eventService = createFixedEventService();
        long officeId = createOffice();

        var eventResult = mvc.perform(post("/api/admin/schedule/events")
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specialistId":%s,
                                  "serviceId":%s,
                                  "officeId":%s,
                                  "startsAt":"2032-04-03T08:00:00Z",
                                  "endsAt":"2032-04-03T09:30:00Z",
                                  "capacity":4,
                                  "active":true
                                }
                                """.formatted(otherSpecialist.getId(), eventService.getId(), officeId)))
                .andExpect(status().isOk())
                .andReturn();
        long eventId = objectMapper.readTree(eventResult.getResponse().getContentAsString()).path("id").asLong();

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(ownerCookies)
                        .param("from", "2032-04-01T00:00:00Z")
                        .param("to", "2032-04-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)]".formatted(ownerBlock.getId())).exists())
                .andExpect(jsonPath("$[?(@.id == %s && @.booked == true)]".formatted(otherBlock.getId())).exists());

        mvc.perform(get("/api/admin/schedule/bookings")
                        .cookie(ownerCookies)
                        .param("from", "2032-04-01T00:00:00Z")
                        .param("to", "2032-04-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)]".formatted(booking.getId())).exists())
                .andExpect(jsonPath("$[?(@.id == %s && @.status == 'CANCELLED')]".formatted(cancelledBooking.getId())).exists());

        mvc.perform(get("/api/admin/schedule/events")
                        .cookie(ownerCookies)
                        .param("from", "2032-04-01T00:00:00Z")
                        .param("to", "2032-04-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)]".formatted(eventId)).exists());
    }

    @Test
    void masterSpecialistCanCopyOtherSpecialistDayPlan() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);
        User otherSpecialist = createUserWithRoles(uniquePhone(), UserRole.SPECIALIST);
        SpecialistAvailabilityBlock sourceBlock = createAvailabilityEntity(
                otherSpecialist,
                "2033-01-02T08:00:00Z",
                "2033-01-02T09:00:00Z"
        );

        mvc.perform(post("/api/admin/schedule/day-copy")
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specialistId":%s,
                                  "sourceDate":"2033-01-02",
                                  "targetDates":["2033-01-03","2033-01-04"],
                                  "includeAvailability":true,
                                  "includeFixedEvents":false
                                }
                                """.formatted(otherSpecialist.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialistId").value(otherSpecialist.getId()))
                .andExpect(jsonPath("$.copiedAvailabilityCount").value(2))
                .andExpect(jsonPath("$.copiedEventCount").value(0))
                .andExpect(jsonPath("$.conflicts").isEmpty());

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(ownerCookies)
                        .param("specialistId", String.valueOf(otherSpecialist.getId()))
                        .param("from", "2033-01-03T00:00:00Z")
                        .param("to", "2033-01-05T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.startsAt == '2033-01-03T08:00:00Z')]").exists())
                .andExpect(jsonPath("$[?(@.startsAt == '2033-01-04T08:00:00Z')]").exists());

        availabilityBlockRepository.delete(sourceBlock);
    }

    @Test
    void regularSpecialistCannotCopyOtherSpecialistDayPlan() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);
        User otherSpecialist = createUserWithRoles(uniquePhone(), UserRole.SPECIALIST);
        User regularSpecialist = createUserWithRoles(uniquePhone(), UserRole.SPECIALIST);
        Cookie[] regularSpecialistCookies = loginCookies(regularSpecialist.getPhone());
        createAvailabilityEntity(
                otherSpecialist,
                "2033-03-02T08:00:00Z",
                "2033-03-02T09:00:00Z"
        );

        mvc.perform(post("/api/admin/schedule/day-copy")
                        .with(csrf())
                        .cookie(regularSpecialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specialistId":%s,
                                  "sourceDate":"2033-03-02",
                                  "targetDates":["2033-03-03"],
                                  "includeAvailability":true,
                                  "includeFixedEvents":false
                                }
                                """.formatted(otherSpecialist.getId())))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(ownerCookies)
                        .param("specialistId", String.valueOf(otherSpecialist.getId()))
                        .param("from", "2033-03-03T00:00:00Z")
                        .param("to", "2033-03-04T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void masterSpecialistCanManageOtherSpecialistEventsButRegularSpecialistCannot() throws Exception {
        Cookie[] ownerCookies = loginCookies(OWNER_PHONE);
        User otherSpecialist = createUserWithRoles(uniquePhone(), UserRole.SPECIALIST);
        User regularSpecialist = createUserWithRoles(uniquePhone(), UserRole.SPECIALIST);
        Cookie[] regularSpecialistCookies = loginCookies(regularSpecialist.getPhone());
        ServiceOffering eventService = createFixedEventService();
        long officeId = createOffice();

        var createResult = mvc.perform(post("/api/admin/schedule/events")
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specialistId":%s,
                                  "serviceId":%s,
                                  "officeId":%s,
                                  "startsAt":"2033-04-02T08:00:00Z",
                                  "endsAt":"2033-04-02T09:30:00Z",
                                  "capacity":4,
                                  "note":"Owner-created event",
                                  "active":true
                                }
                                """.formatted(otherSpecialist.getId(), eventService.getId(), officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialistId").value(otherSpecialist.getId()))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        long eventId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        mvc.perform(get("/api/admin/schedule/events")
                        .cookie(ownerCookies)
                        .param("specialistId", String.valueOf(otherSpecialist.getId()))
                        .param("from", "2033-04-01T00:00:00Z")
                        .param("to", "2033-04-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eventId));

        mvc.perform(get("/api/admin/schedule/events")
                        .cookie(regularSpecialistCookies)
                        .param("specialistId", String.valueOf(otherSpecialist.getId()))
                        .param("from", "2033-04-01T00:00:00Z")
                        .param("to", "2033-04-08T00:00:00Z"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/schedule/events/enrollments")
                        .cookie(regularSpecialistCookies)
                        .param("specialistId", String.valueOf(otherSpecialist.getId()))
                        .param("from", "2033-04-01T00:00:00Z")
                        .param("to", "2033-04-08T00:00:00Z"))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/admin/schedule/events/{id}", eventId)
                        .with(csrf())
                        .cookie(regularSpecialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceId":%s,
                                  "officeId":%s,
                                  "startsAt":"2033-04-02T08:00:00Z",
                                  "endsAt":"2033-04-02T09:30:00Z",
                                  "capacity":4,
                                  "note":"Regular specialist edit attempt",
                                  "active":false
                                }
                                """.formatted(eventService.getId(), officeId)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/admin/schedule/events/{id}", eventId)
                        .with(csrf())
                        .cookie(ownerCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceId":%s,
                                  "officeId":%s,
                                  "startsAt":"2033-04-02T08:00:00Z",
                                  "endsAt":"2033-04-02T09:30:00Z",
                                  "capacity":5,
                                  "note":"Owner updated event",
                                  "active":true
                                }
                                """.formatted(eventService.getId(), officeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(5))
                .andExpect(jsonPath("$.note").value("Owner updated event"));
    }

    @Test
    void fixedEventRequiresThirtyMinuteBufferFromExistingBooking() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        User client = createUserWithRoles(uniquePhone());
        SpecialistAvailabilityBlock block = createAvailabilityEntity(
                specialist,
                "2033-05-02T08:00:00Z",
                "2033-05-02T09:00:00Z"
        );
        ServiceOffering bookingService = createService();
        createBookingEntity(client, specialist, block, bookingService);
        ServiceOffering eventService = createFixedEventService();
        long officeId = createOffice();

        mvc.perform(post("/api/admin/schedule/events")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceId":%s,
                                  "officeId":%s,
                                  "startsAt":"2033-05-02T09:15:00Z",
                                  "endsAt":"2033-05-02T10:15:00Z",
                                  "capacity":4,
                                  "active":true
                                }
                                """.formatted(eventService.getId(), officeId)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/schedule/events")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceId":%s,
                                  "officeId":%s,
                                  "startsAt":"2033-05-02T09:30:00Z",
                                  "endsAt":"2033-05-02T10:30:00Z",
                                  "capacity":4,
                                  "active":true
                                }
                                """.formatted(eventService.getId(), officeId)))
                .andExpect(status().isOk());
    }

    @Test
    void dayPlanCopyReportsConflictsWithoutPartialCopy() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        createAvailabilityEntity(specialist, "2033-02-02T08:00:00Z", "2033-02-02T09:00:00Z");
        createAvailabilityEntity(specialist, "2033-02-03T08:30:00Z", "2033-02-03T09:30:00Z");

        mvc.perform(post("/api/admin/schedule/day-copy")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceDate":"2033-02-02",
                                  "targetDates":["2033-02-03"],
                                  "includeAvailability":true,
                                  "includeFixedEvents":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.copiedAvailabilityCount").value(0))
                .andExpect(jsonPath("$.conflicts[0].targetDate").value("2033-02-03"));

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(specialistCookies)
                        .param("from", "2033-02-03T00:00:00Z")
                        .param("to", "2033-02-04T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void dayPlanCopyReportsBufferConflictsWithExistingBookings() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        User client = createUserWithRoles(uniquePhone());
        ServiceOffering service = createService();
        SpecialistAvailabilityBlock sourceBlock = createAvailabilityEntity(
                specialist,
                "2033-06-02T09:15:00Z",
                "2033-06-02T10:15:00Z"
        );
        sourceBlock.setItemType(com.example.visceralmassageapi.schedule.domain.ScheduleBlockType.APPOINTMENT_SLOT);
        sourceBlock.setService(service);
        sourceBlock.setCapacity(1);
        availabilityBlockRepository.save(sourceBlock);
        SpecialistAvailabilityBlock targetBookedBlock = createAvailabilityEntity(
                specialist,
                "2033-06-03T08:00:00Z",
                "2033-06-03T09:00:00Z"
        );
        createBookingEntity(client, specialist, targetBookedBlock, service);

        mvc.perform(post("/api/admin/schedule/day-copy")
                        .with(csrf())
                        .cookie(specialistCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceDate":"2033-06-02",
                                  "targetDates":["2033-06-03"],
                                  "includeAvailability":true,
                                  "includeFixedEvents":false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.copiedAvailabilityCount").value(0))
                .andExpect(jsonPath("$.conflicts[0].targetDate").value("2033-06-03"))
                .andExpect(jsonPath("$.conflicts[0].reason").value("is too close to existing booking"));

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(specialistCookies)
                        .param("from", "2033-06-03T00:00:00Z")
                        .param("to", "2033-06-04T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void specialistCannotDeleteBlockWithBookingHistory() throws Exception {
        Cookie[] specialistCookies = loginCookies(OWNER_PHONE);
        User specialist = userRepository.findByPhone(OWNER_PHONE).orElseThrow();
        User client = createUserWithRoles(uniquePhone());
        SpecialistAvailabilityBlock block = createAvailabilityEntity(specialist);
        Booking booking = new Booking();
        booking.setUser(client);
        booking.setSpecialist(specialist);
        ServiceOffering service = createService();
        booking.setService(service);
        booking.setAvailabilityBlock(block);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStartsAt(block.getStartsAt());
        booking.setEndsAt(block.getEndsAt());
        booking.setBookedPrice(service.getBasePrice());
        bookingRepository.save(booking);

        mvc.perform(delete("/api/admin/schedule/availability/{id}", block.getId())
                        .with(csrf())
                        .cookie(specialistCookies))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/admin/schedule/availability")
                        .cookie(specialistCookies)
                        .param("from", "2032-01-01T00:00:00Z")
                        .param("to", "2032-01-08T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)].booked".formatted(block.getId())).value(false));
    }

    private long createOffice() {
        Office office = new Office();
        office.setName("Schedule Office " + PHONE_SUFFIX.incrementAndGet());
        office.setAddress("Kyiv");
        office.setActive(true);
        return officeRepository.save(office).getId();
    }

    private void createAvailability(Cookie[] cookies, long officeId, String status, String startsAt, String endsAt) throws Exception {
        mvc.perform(post("/api/admin/schedule/availability")
                        .with(csrf())
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "officeId":%s,
                                  "status":"%s",
                                  "startsAt":"%s",
                                  "endsAt":"%s"
                                }
                                """.formatted(officeId, status, startsAt, endsAt)))
                .andExpect(status().isOk());
    }

    private User createUserWithRoles(String phone, UserRole... roles) {
        User user = new User();
        user.setPhone(phone);
        user.setFirstName("Schedule");
        user.setLastName("User");
        user.setPasswordHash(passwordEncoder.encode(OWNER_PASSWORD));
        user.getRoles().add(UserRole.USER);
        for (UserRole role : roles) {
            user.getRoles().add(role);
        }
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private SpecialistAvailabilityBlock createAvailabilityEntity(User specialist) {
        return createAvailabilityEntity(specialist, "2032-01-02T08:00:00Z", "2032-01-02T09:00:00Z");
    }

    private SpecialistAvailabilityBlock createAvailabilityEntity(User specialist, String startsAt, String endsAt) {
        SpecialistAvailabilityBlock block = new SpecialistAvailabilityBlock();
        block.setSpecialist(specialist);
        block.setStatus(com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus.AVAILABLE);
        block.setItemType(com.example.visceralmassageapi.schedule.domain.ScheduleBlockType.OPEN_RANGE);
        block.setStartsAt(java.time.OffsetDateTime.parse(startsAt));
        block.setEndsAt(java.time.OffsetDateTime.parse(endsAt));
        return availabilityBlockRepository.save(block);
    }

    private ServiceOffering createService() {
        ServiceOffering service = new ServiceOffering();
        service.setTitleUa("Schedule history service " + PHONE_SUFFIX.incrementAndGet());
        service.setDescriptionUa("Test");
        service.setDurationMinutes(60);
        service.setBasePrice(BigDecimal.valueOf(1000));
        service.setActive(true);
        return serviceOfferingRepository.save(service);
    }

    private ServiceOffering createFixedEventService() {
        ServiceOffering service = new ServiceOffering();
        service.setTitleUa("Schedule event service " + PHONE_SUFFIX.incrementAndGet());
        service.setDescriptionUa("Test event");
        service.setDurationMinutes(90);
        service.setBasePrice(BigDecimal.valueOf(700));
        service.setBookingMode(ServiceBookingMode.FIXED_EVENT);
        service.setActive(true);
        return serviceOfferingRepository.save(service);
    }

    private Booking createBookingEntity(
            User client,
            User specialist,
            SpecialistAvailabilityBlock block,
            ServiceOffering service
    ) {
        Booking booking = new Booking();
        booking.setUser(client);
        booking.setSpecialist(specialist);
        booking.setService(service);
        booking.setAvailabilityBlock(block);
        booking.setStatus(BookingStatus.AWAITING_PAYMENT_CONFIRMATION);
        booking.setStartsAt(block.getStartsAt());
        booking.setEndsAt(block.getEndsAt());
        booking.setBookedPrice(service.getBasePrice());
        return bookingRepository.save(booking);
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
        return "+38096" + PHONE_SUFFIX.incrementAndGet();
    }
}
