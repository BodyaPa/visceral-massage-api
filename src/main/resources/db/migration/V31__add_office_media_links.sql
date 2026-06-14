ALTER TABLE offices
    DROP COLUMN description,
    ADD COLUMN google_maps_url VARCHAR(2048);
