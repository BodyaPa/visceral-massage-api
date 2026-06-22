ALTER TABLE users
    ADD COLUMN avatar_media_id UUID REFERENCES media_assets (id) ON DELETE SET NULL;
