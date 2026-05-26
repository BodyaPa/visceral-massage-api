ALTER TABLE news
    ADD COLUMN cover_display_mode VARCHAR(16) NOT NULL DEFAULT 'FILL';

ALTER TABLE news
    ADD CONSTRAINT ck_news_cover_display_mode CHECK (cover_display_mode IN ('FILL', 'FIT'));
