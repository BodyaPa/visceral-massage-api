ALTER TABLE bookings
    ADD COLUMN reminder_sent_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE fixed_event_enrollments
    ADD COLUMN reminder_sent_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_bookings_due_reminders
    ON bookings(reminder_opt_in, reminder_sent_at, status, starts_at);

CREATE INDEX idx_fixed_event_enrollments_due_reminders
    ON fixed_event_enrollments(reminder_opt_in, reminder_sent_at, status);
