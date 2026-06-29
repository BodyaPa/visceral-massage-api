CREATE TABLE membership_offer_services (
    offer_id BIGINT NOT NULL REFERENCES membership_offers(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    PRIMARY KEY (offer_id, service_id)
);

INSERT INTO membership_offer_services (offer_id, service_id)
SELECT offer.id, service.id
FROM membership_offers offer
CROSS JOIN services service
WHERE offer.kind = 'MEMBERSHIP'
  AND service.booking_mode = 'INDIVIDUAL_APPOINTMENT';

ALTER TABLE bookings
    ADD COLUMN membership_purchase_id BIGINT REFERENCES membership_purchases(id);

ALTER TABLE fixed_event_enrollments
    ADD COLUMN membership_purchase_id BIGINT REFERENCES membership_purchases(id);

CREATE INDEX idx_bookings_membership_purchase ON bookings(membership_purchase_id);
CREATE INDEX idx_fixed_event_enrollments_membership_purchase ON fixed_event_enrollments(membership_purchase_id);
