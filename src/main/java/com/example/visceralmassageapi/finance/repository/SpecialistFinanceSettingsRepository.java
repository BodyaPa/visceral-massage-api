package com.example.visceralmassageapi.finance.repository;

import com.example.visceralmassageapi.finance.domain.SpecialistFinanceSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SpecialistFinanceSettingsRepository extends JpaRepository<SpecialistFinanceSettings, Long> {
    List<SpecialistFinanceSettings> findBySpecialistUserIdIn(Collection<Long> specialistIds);
}
