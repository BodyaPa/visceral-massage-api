package com.example.visceralmassageapi.media.repository;

import com.example.visceralmassageapi.media.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    List<MediaAsset> findAllByNewsIdOrderByCreatedAtAsc(Integer newsId);

    Optional<MediaAsset> findByIdAndNewsId(UUID id, Integer newsId);
}
