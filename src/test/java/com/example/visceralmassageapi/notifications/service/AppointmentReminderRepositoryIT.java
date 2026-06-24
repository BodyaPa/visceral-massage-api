package com.example.visceralmassageapi.notifications.service;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.schedule.domain.FixedEvent;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollment;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;
import com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus;
import com.example.visceralmassageapi.schedule.domain.SpecialistAvailabilityBlock;
import com.example.visceralmassageapi.schedule.repository.FixedEventEnrollmentRepository;
import com.example.visceralmassageapi.schedule.repository.FixedEventRepository;
import com.example.visceralmassageapi.schedule.repository.SpecialistAvailabilityBlockRepository;
import com.example.visceralmassageapi.services.entity.ServiceBookingMode;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import com.example.visceralmassageapi.services.repository.ServiceOfferingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentReminderRepositoryIT extends IntegrationTestBase {

    private static final AtomicInteger SUFFIX = new AtomicInteger(9000000);

    @Autowired UserRepository userRepository;
    @Autowired ServiceOfferingRepository serviceOfferingRepository;
    @Autowired SpecialistAvailabilityBlockRepository availabilityBlockRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired FixedEventRepository fixedEventRepository;
    @Autowired FixedEventEnrollmentRepository enrollmentRepository;

    @Test
    void appointmentReminderQueryReturnsOnlyOptedInUnsentConfirmedBookingsInWindow() {
        OffsetDateTime now = OffsetDateTime.parse("2036-02-01T08:00:00Z");
        User client = createUser(UserRole.USER);
        User specialist = createUser(UserRole.SPECIALIST);
        ServiceOffering service = createService(ServiceBookingMode.INDIVIDUAL_APPOINTMENT);

        Booking eligible = createBooking(client, specialist, service, BookingStatus.CONFIRMED, true, null, now.plusMinutes(45));
        createBooking(client, specialist, service, BookingStatus.CONFIRMED, false, null, now.plusMinutes(45));
        createBooking(client, specialist, service, BookingStatus.AWAITING_PAYMENT_CONFIRMATION, true, null, now.plusMinutes(45));
        createBooking(client, specialist, service, BookingStatus.CANCELLED, true, null, now.plusMinutes(45));
        createBooking(client, specialist, service, BookingStatus.CONFIRMED, true, now.minusMinutes(1), now.plusMinutes(45));
        createBooking(client, specialist, service, BookingStatus.CONFIRMED, true, null, now.plusHours(3));

        assertThat(bookingRepository.findDueAppointmentReminders(now, now.plusHours(2)))
                .extracting(Booking::getId)
                .containsExactly(eligible.getId());
    }

    @Test
    void eventReminderQueryReturnsOnlyOptedInUnsentActiveEnrollmentsForActiveEventsInWindow() {
        OffsetDateTime now = OffsetDateTime.parse("2036-02-01T08:00:00Z");
        User client = createUser(UserRole.USER);
        User specialist = createUser(UserRole.SPECIALIST);
        ServiceOffering service = createService(ServiceBookingMode.FIXED_EVENT);

        FixedEventEnrollment eligible = createEnrollment(client, specialist, service, true, FixedEventEnrollmentStatus.ACTIVE, true, null, now.plusMinutes(45));
        createEnrollment(client, specialist, service, false, FixedEventEnrollmentStatus.ACTIVE, true, null, now.plusMinutes(45));
        createEnrollment(client, specialist, service, true, FixedEventEnrollmentStatus.CANCELLED, true, null, now.plusMinutes(45));
        createEnrollment(client, specialist, service, true, FixedEventEnrollmentStatus.ACTIVE, false, null, now.plusMinutes(45));
        createEnrollment(client, specialist, service, true, FixedEventEnrollmentStatus.ACTIVE, true, now.minusMinutes(1), now.plusMinutes(45));
        createEnrollment(client, specialist, service, true, FixedEventEnrollmentStatus.ACTIVE, true, null, now.plusHours(3));

        assertThat(enrollmentRepository.findDueEventReminders(now, now.plusHours(2)))
                .extracting(FixedEventEnrollment::getId)
                .containsExactly(eligible.getId());
    }

    private User createUser(UserRole role) {
        User user = new User();
        user.setPhone("+38095" + SUFFIX.incrementAndGet());
        user.setEmail("reminder-" + SUFFIX.incrementAndGet() + "@example.com");
        user.setFirstName("Reminder");
        user.setLastName("User");
        user.setPasswordHash("test-password-hash");
        user.getRoles().add(UserRole.USER);
        user.getRoles().add(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private ServiceOffering createService(ServiceBookingMode bookingMode) {
        ServiceOffering service = new ServiceOffering();
        service.setTitleUa("Reminder service " + SUFFIX.incrementAndGet());
        service.setDescriptionUa("Reminder test");
        service.setDurationMinutes(60);
        service.setBasePrice(BigDecimal.valueOf(1000));
        service.setBookingMode(bookingMode);
        service.setActive(true);
        return serviceOfferingRepository.save(service);
    }

    private Booking createBooking(
            User client,
            User specialist,
            ServiceOffering service,
            BookingStatus status,
            boolean reminderOptIn,
            OffsetDateTime reminderSentAt,
            OffsetDateTime startsAt
    ) {
        SpecialistAvailabilityBlock block = new SpecialistAvailabilityBlock();
        block.setSpecialist(specialist);
        block.setStatus(ScheduleBlockStatus.AVAILABLE);
        block.setStartsAt(startsAt);
        block.setEndsAt(startsAt.plusHours(1));
        block = availabilityBlockRepository.save(block);

        Booking booking = new Booking();
        booking.setUser(client);
        booking.setSpecialist(specialist);
        booking.setService(service);
        booking.setAvailabilityBlock(block);
        booking.setStatus(status);
        booking.setStartsAt(startsAt);
        booking.setEndsAt(startsAt.plusHours(1));
        booking.setBookedPrice(service.getBasePrice());
        booking.setReminderOptIn(reminderOptIn);
        booking.setReminderSentAt(reminderSentAt);
        return bookingRepository.save(booking);
    }

    private FixedEventEnrollment createEnrollment(
            User client,
            User specialist,
            ServiceOffering service,
            boolean reminderOptIn,
            FixedEventEnrollmentStatus status,
            boolean eventActive,
            OffsetDateTime reminderSentAt,
            OffsetDateTime startsAt
    ) {
        FixedEvent event = new FixedEvent();
        event.setService(service);
        event.setSpecialist(specialist);
        event.setStartsAt(startsAt);
        event.setEndsAt(startsAt.plusHours(1));
        event.setCapacity(4);
        event.setActive(eventActive);
        event = fixedEventRepository.save(event);

        FixedEventEnrollment enrollment = new FixedEventEnrollment();
        enrollment.setEvent(event);
        enrollment.setUser(client);
        enrollment.setStatus(status);
        enrollment.setReminderOptIn(reminderOptIn);
        enrollment.setReminderSentAt(reminderSentAt);
        return enrollmentRepository.save(enrollment);
    }
}
