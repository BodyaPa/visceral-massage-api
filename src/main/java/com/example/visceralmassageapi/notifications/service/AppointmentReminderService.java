package com.example.visceralmassageapi.notifications.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.schedule.domain.FixedEvent;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollment;
import com.example.visceralmassageapi.schedule.repository.FixedEventEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AppointmentReminderService {

    private static final int REMINDER_WINDOW_HOURS = 2;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingRepository bookingRepository;
    private final FixedEventEnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    @Transactional
    public void sendDueReminders(OffsetDateTime now) {
        OffsetDateTime remindBefore = now.plusHours(REMINDER_WINDOW_HOURS);
        bookingRepository.findDueAppointmentReminders(now, remindBefore)
                .forEach(booking -> sendBookingReminder(booking, now));
        enrollmentRepository.findDueEventReminders(now, remindBefore)
                .forEach(enrollment -> sendEventReminder(enrollment, now));
    }

    private void sendBookingReminder(Booking booking, OffsetDateTime sentAt) {
        User user = booking.getUser();
        if (user.getEmail() != null) {
            notificationService.sendEmail(
                    user.getEmail(),
                    "Ataraksia appointment reminder",
                    """
                            Reminder for your Ataraksia appointment.

                            Service: %s
                            Time: %s
                            Office: %s
                            Address: %s
                            """.formatted(
                            booking.getService().getTitleUa(),
                            formatDateTime(booking.getStartsAt()),
                            officeName(booking.getOffice()),
                            officeAddress(booking.getOffice())
                    )
            );
        }
        booking.setReminderSentAt(sentAt);
    }

    private void sendEventReminder(FixedEventEnrollment enrollment, OffsetDateTime sentAt) {
        User user = enrollment.getUser();
        FixedEvent event = enrollment.getEvent();
        if (user.getEmail() != null) {
            notificationService.sendEmail(
                    user.getEmail(),
                    "Ataraksia event reminder",
                    """
                            Reminder for your Ataraksia event.

                            Event: %s
                            Time: %s
                            Office: %s
                            Address: %s
                            """.formatted(
                            event.getService().getTitleUa(),
                            formatDateTime(event.getStartsAt()),
                            officeName(event.getOffice()),
                            officeAddress(event.getOffice())
                    )
            );
        }
        enrollment.setReminderSentAt(sentAt);
    }

    private String formatDateTime(OffsetDateTime value) {
        return value.format(DATE_TIME_FORMATTER);
    }

    private String officeName(Office office) {
        return office == null ? "No office assigned" : office.getName();
    }

    private String officeAddress(Office office) {
        return office == null ? "No office address" : office.getAddress();
    }
}
