package com.example.visceralmassageapi.news.repository;

import com.example.visceralmassageapi.news.entity.NewsItem;
import com.example.visceralmassageapi.news.entity.NewsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NewsRepository extends JpaRepository<NewsItem, Integer> {
    @Query("""
            SELECT item FROM NewsItem item
            WHERE item.status = :status
              AND ((:locale = 'ua' AND TRIM(COALESCE(item.titleUa, '')) <> '' AND TRIM(COALESCE(item.contentUa, '')) <> '')
                OR (:locale = 'en' AND TRIM(COALESCE(item.titleEn, '')) <> '' AND TRIM(COALESCE(item.contentEn, '')) <> ''))
            """)
    Page<NewsItem> findPublishedForLocale(@Param("status") NewsStatus status, @Param("locale") String locale, Pageable pageable);

    @Query("""
            SELECT item FROM NewsItem item
            WHERE item.id = :id
              AND item.status = :status
              AND ((:locale = 'ua' AND TRIM(COALESCE(item.titleUa, '')) <> '' AND TRIM(COALESCE(item.contentUa, '')) <> '')
                OR (:locale = 'en' AND TRIM(COALESCE(item.titleEn, '')) <> '' AND TRIM(COALESCE(item.contentEn, '')) <> ''))
            """)
    Optional<NewsItem> findPublishedForLocale(@Param("id") Integer id, @Param("status") NewsStatus status, @Param("locale") String locale);
}
