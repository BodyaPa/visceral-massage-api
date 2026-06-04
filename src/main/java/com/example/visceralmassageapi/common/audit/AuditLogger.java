package com.example.visceralmassageapi.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditLogger {

    public void adminBootstrapCreated() {
        log.info("audit event=admin_bootstrap_created");
    }

    public void userRegistered(long userId) {
        log.info("audit event=user_registered userId={}", userId);
    }

    public void loginSucceeded(long userId) {
        log.info("audit event=login_succeeded userId={}", userId);
    }

    public void loginFailed() {
        log.warn("audit event=login_failed");
    }

    public void adminAccessDenied(String method) {
        log.warn("audit event=admin_access_denied area=admin_api method={}", method);
    }

    public void bookingCreated(long bookingId, long actorId) {
        log.info("audit event=booking_created bookingId={} actorId={}", bookingId, actorId);
    }

    public void bookingConflict(long availabilityBlockId, long actorId) {
        log.warn("audit event=booking_conflict availabilityBlockId={} actorId={}", availabilityBlockId, actorId);
    }

    public void bookingCancelled(long bookingId, long actorId) {
        log.info("audit event=booking_cancelled bookingId={} actorId={}", bookingId, actorId);
    }

    public void bookingPaymentConfirmed(long bookingId, long actorId) {
        log.info("audit event=booking_payment_confirmed bookingId={} actorId={}", bookingId, actorId);
    }
}
