package com.example.visceralmassageapi.booking.repository;

import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;

public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<Booking> financeFilter(
            BookingStatus status,
            Long officeId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (officeId != null) {
                predicates.add(builder.equal(root.get("office").get("id"), officeId));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("startsAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThan(root.get("startsAt"), to));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
