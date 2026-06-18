package com.example.visceralmassageapi.site.repository;

import com.example.visceralmassageapi.site.domain.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingsRepository extends JpaRepository<SiteSettings, Short> {
}
