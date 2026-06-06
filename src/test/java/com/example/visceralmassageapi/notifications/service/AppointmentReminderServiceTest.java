package com.example.visceralmassageapi.notifications.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.offices.entity.Office;
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
}
