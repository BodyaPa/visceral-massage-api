package com.example.visceralmassageapi.services.repository;

import com.example.visceralmassageapi.services.entity.ServiceOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    @Query(value = """
            SELECT service
            FROM ServiceOffering service
            WHERE (:query IS NULL
                OR LOWER(service.titleUa) LIKE :query
                OR LOWER(COALESCE(service.titleEn, '')) LIKE :query)
            AND (:active IS NULL OR service.active = :active)
            """,
            countQuery = """
            SELECT COUNT(service)
            FROM ServiceOffering service
            WHERE (:query IS NULL
                OR LOWER(service.titleUa) LIKE :query
                OR LOWER(COALESCE(service.titleEn, '')) LIKE :query)
            AND (:active IS NULL OR service.active = :active)
            """)
    Page<ServiceOffering> searchAdmin(String query, Boolean active, Pageable pageable);

    @Query(value = """
            SELECT service
            FROM ServiceOffering service
            WHERE service.active = TRUE
            AND (:locale = 'UA' AND service.titleUa IS NOT NULL AND TRIM(service.titleUa) <> ''
                OR :locale = 'EN' AND service.titleEn IS NOT NULL AND TRIM(service.titleEn) <> '')
            """,
            countQuery = """
            SELECT COUNT(service)
            FROM ServiceOffering service
            WHERE service.active = TRUE
            AND (:locale = 'UA' AND service.titleUa IS NOT NULL AND TRIM(service.titleUa) <> ''
                OR :locale = 'EN' AND service.titleEn IS NOT NULL AND TRIM(service.titleEn) <> '')
            """)
    Page<ServiceOffering> findPublic(String locale, Pageable pageable);
}
