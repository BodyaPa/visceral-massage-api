package com.example.visceralmassageapi.finance.repository;

import com.example.visceralmassageapi.finance.domain.FinanceExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FinanceExpenseRepository extends JpaRepository<FinanceExpense, Long>, JpaSpecificationExecutor<FinanceExpense> {
}
