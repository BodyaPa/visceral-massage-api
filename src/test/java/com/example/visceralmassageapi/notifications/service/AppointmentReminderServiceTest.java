package com.example.visceralmassageapi.notifications.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.schedule.domain.FixedEvent;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollment;
import com.example.visceralmassageapi.schedule.repository.FixedEventEnrollmentRepository;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock FixedEventEnrollmentRepository enrollmentRepository;
    @Mock NotificationService notificationService;

    @InjectMocks AppointmentReminderService reminderService;

    @Test
    void sendsDueBookingReminderToEmailAndMarksSent() {
        OffsetDateTime now = OffsetDateTime.parse("2031-01-10T08:00:00Z");
        Booking booking = booking("client@example.com");
        when(bookingRepository.findDueAppointmentReminders(now, now.plusHours(2))).thenReturn(List.of(booking));
        when(enrollmentRepository.findDueEventReminders(now, now.plusHours(2))).thenReturn(List.of());

        reminderService.sendDueReminders(now);

        verify(notificationService).sendEmail(
                contains("client@example.com"),
                contains("appointment reminder"),
                contains("Visceral massage")
        );
        assertThat(booking.getReminderSentAt()).isEqualTo(now);
    }

    @Test
    void marksPhoneOnlyBookingSentWithoutEmailUntilSmsProviderExists() {
        OffsetDateTime now = OffsetDateTime.parse("2031-01-10T08:00:00Z");
        Booking booking = booking(null);
        when(bookingRepository.findDueAppointmentReminders(now, now.plusHours(2))).thenReturn(List.of(booking));
        when(enrollmentRepository.findDueEventReminders(now, now.plusHours(2))).thenReturn(List.of());

        reminderService.sendDueReminders(now);

        verify(notificationService, never()).sendEmail(any(), any(), any());
        assertThat(booking.getReminderSentAt()).isEqualTo(now);
    }

    @Test
    void sendsDueEventReminderToEmailAndMarksSent() {
        OffsetDateTime now = OffsetDateTime.parse("2031-01-10T08:00:00Z");
        FixedEventEnrollment enrollment = enrollment("client@example.com");
        when(bookingRepository.findDueAppointmentReminders(now, now.plusHours(2))).thenReturn(List.of());
        when(enrollmentRepository.findDueEventReminders(now, now.plusHours(2))).thenReturn(List.of(enrollment));

        reminderService.sendDueReminders(now);

        verify(notificationService).sendEmail(
                contains("client@example.com"),
                contains("event reminder"),
                contains("Group session")
        );
        assertThat(enrollment.getReminderSentAt()).isEqualTo(now);
    }

    @Test
    void continuesSendingRemainingRemindersWhenOneEmailFails() {
        OffsetDateTime now = OffsetDateTime.parse("2031-01-10T08:00:00Z");
        Booking failed = booking("fail@example.com");
        Booking delivered = booking("ok@example.com");
        when(bookingRepository.findDueAppointmentReminders(now, now.plusHours(2))).thenReturn(List.of(failed, delivered));
        when(enrollmentRepository.findDueEventReminders(now, now.plusHours(2))).thenReturn(List.of());
        doThrow(new RuntimeException("smtp down"))
                .when(notificationService)
                .sendEmail(eq("fail@example.com"), any(), any());

        reminderService.sendDueReminders(now);

        verify(notificationService).sendEmail(eq("ok@example.com"), contains("appointment reminder"), contains("Visceral massage"));
        assertThat(failed.getReminderSentAt()).isNull();
        assertThat(delivered.getReminderSentAt()).isEqualTo(now);
    }

    private Booking booking(String email) {
        User user = new User();
        user.setEmail(email);

        ServiceOffering service = new ServiceOffering();
        service.setTitleUa("Visceral massage");

        Office office = new Office();
        office.setName("Office 1");
        office.setAddress("Kyiv, Test street 1");

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setService(service);
        booking.setOffice(office);
        booking.setStartsAt(OffsetDateTime.parse("2031-01-10T09:30:00Z"));
        return booking;
    }

    private FixedEventEnrollment enrollment(String email) {
        User user = new User();
        user.setEmail(email);

        ServiceOffering service = new ServiceOffering();
        service.setTitleUa("Group session");

        Office office = new Office();
        office.setName("Office 2");
        office.setAddress("Lviv, Test street 2");

        FixedEvent event = new FixedEvent();
        event.setService(service);
        event.setOffice(office);
        event.setStartsAt(OffsetDateTime.parse("2031-01-10T09:45:00Z"));

        FixedEventEnrollment enrollment = new FixedEventEnrollment();
        enrollment.setUser(user);
        enrollment.setEvent(event);
        return enrollment;
    }
}
