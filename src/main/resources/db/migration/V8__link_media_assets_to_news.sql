ALTER TABLE media_assets
    ADD COLUMN news_id INTEGER REFERENCES news (id) ON DELETE SET NULL;

CREATE INDEX ix_media_assets_news_id ON media_assets (news_id);
