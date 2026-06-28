package com.example.visceralmassageapi.media.repository;

import com.example.visceralmassageapi.media.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    List<MediaAsset> findAllByNewsIdOrderByCreatedAtAsc(Integer newsId);

    List<MediaAsset> findAllByOfficeIdOrderByCreatedAtAsc(Long officeId);

    List<MediaAsset> findAllBySiteSettingsIdOrderBySiteSliderSortOrderAscCreatedAtAsc(Short siteSettingsId);

    Optional<MediaAsset> findByIdAndNewsId(UUID id, Integer newsId);

    Optional<MediaAsset> findByIdAndSiteSettingsId(UUID id, Short siteSettingsId);

    @Query("select coalesce(max(m.siteSliderSortOrder), -1) from MediaAsset m where m.siteSettingsId = ?1")
    int maxSiteSliderSortOrder(Short siteSettingsId);
}
