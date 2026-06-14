package com.example.visceralmassageapi.offices.repository;

import com.example.visceralmassageapi.offices.entity.Office;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OfficeRepository extends JpaRepository<Office, Long> {

    @Query(value = """
            SELECT office
            FROM Office office
            WHERE (:query IS NULL
                OR LOWER(office.name) LIKE :query
                OR LOWER(office.address) LIKE :query)
            AND (:active IS NULL OR office.active = :active)
            """,
            countQuery = """
            SELECT COUNT(office)
            FROM Office office
            WHERE (:query IS NULL
                OR LOWER(office.name) LIKE :query
                OR LOWER(office.address) LIKE :query)
            AND (:active IS NULL OR office.active = :active)
            """)
    Page<Office> search(String query, Boolean active, Pageable pageable);
}
