package com.example.visceralmassageapi.finance.repository;

import com.example.visceralmassageapi.finance.domain.FinanceExpense;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;

public final class FinanceExpenseSpecifications {

    private FinanceExpenseSpecifications() {
    }

    public static Specification<FinanceExpense> financeFilter(Long officeId, LocalDate from, LocalDate to) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (officeId != null) {
                predicates.add(builder.equal(root.get("office").get("id"), officeId));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("expenseDate"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("expenseDate"), to));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
