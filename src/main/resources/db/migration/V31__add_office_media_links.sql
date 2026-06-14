ALTER TABLE offices
    DROP COLUMN description,
    ADD COLUMN photo_url VARCHAR(2048),
    ADD COLUMN video_url VARCHAR(2048),
    ADD COLUMN google_maps_url VARCHAR(2048);
