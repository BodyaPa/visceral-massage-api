package com.example.visceralmassageapi.finance.repository;

import com.example.visceralmassageapi.finance.domain.FinanceSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceSettingsRepository extends JpaRepository<FinanceSettings, Short> {
}
