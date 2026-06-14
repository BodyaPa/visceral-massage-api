ALTER TABLE media_assets
    ADD COLUMN office_id BIGINT REFERENCES offices (id) ON DELETE SET NULL;

CREATE INDEX ix_media_assets_office_id ON media_assets (office_id);

ALTER TABLE offices
    DROP COLUMN photo_url,
    DROP COLUMN video_url,
    DROP COLUMN google_maps_url,
    ADD COLUMN photo_media_id UUID REFERENCES media_assets (id) ON DELETE SET NULL,
    ADD COLUMN video_media_id UUID REFERENCES media_assets (id) ON DELETE SET NULL;
