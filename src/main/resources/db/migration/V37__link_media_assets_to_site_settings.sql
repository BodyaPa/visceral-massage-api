ALTER TABLE media_assets
    ADD COLUMN site_settings_id SMALLINT,
    ADD COLUMN site_slider_sort_order INTEGER;

CREATE INDEX idx_media_assets_site_settings
    ON media_assets(site_settings_id, site_slider_sort_order, created_at);
