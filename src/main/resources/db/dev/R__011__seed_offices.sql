WITH seed(name, address, active, location_details, directions, google_maps_url) AS (
    VALUES
        ('Ataraksia Center', 'Kyiv, 10 Testova Street', TRUE, 'Second floor, room 204. Dev test office.', 'Use the main entrance and take the stairs to the second floor.', 'https://maps.google.com/?q=Kyiv+10+Testova+Street'),
        ('Ataraksia Studio', 'Kyiv, 25 Spokiina Street', TRUE, 'Ground floor, entrance from the courtyard. Dev test office.', 'Enter through the courtyard gate and follow the Ataraksia sign.', 'https://maps.google.com/?q=Kyiv+25+Spokiina+Street'),
        ('Ataraksia Archive Office', 'Kyiv, 1 Arkhivna Street', FALSE, 'Inactive dev office used to verify status filters.', 'Inactive location for dev status filters.', 'https://maps.google.com/?q=Kyiv+1+Arkhivna+Street'),
        ('Ataraksia Podil Room', 'Kyiv, 14 Podilska Street', TRUE, 'Quiet room for evening appointments. Dev extra office.', 'Ring the Podil room bell and wait near the reception desk.', 'https://maps.google.com/?q=Kyiv+14+Podilska+Street'),
        ('Ataraksia Lviv Pop-up', 'Lviv, 7 Rynok Square', TRUE, 'Temporary pop-up location used for cross-office filters.', 'Use the side entrance from the square.', 'https://maps.google.com/?q=Lviv+7+Rynok+Square'),
        ('Ataraksia Maintenance Room', 'Kyiv, 99 Service Lane', FALSE, 'Inactive maintenance room for negative booking checks.', 'Inactive maintenance location.', 'https://maps.google.com/?q=Kyiv+99+Service+Lane')
)
UPDATE offices office
SET address = seed.address,
    active = seed.active,
    location_details = seed.location_details,
    directions = seed.directions,
    google_maps_url = seed.google_maps_url,
    updated_at = NOW()
FROM seed
WHERE office.name = seed.name;

WITH seed(name, address, active, location_details, directions, google_maps_url) AS (
    VALUES
        ('Ataraksia Center', 'Kyiv, 10 Testova Street', TRUE, 'Second floor, room 204. Dev test office.', 'Use the main entrance and take the stairs to the second floor.', 'https://maps.google.com/?q=Kyiv+10+Testova+Street'),
        ('Ataraksia Studio', 'Kyiv, 25 Spokiina Street', TRUE, 'Ground floor, entrance from the courtyard. Dev test office.', 'Enter through the courtyard gate and follow the Ataraksia sign.', 'https://maps.google.com/?q=Kyiv+25+Spokiina+Street'),
        ('Ataraksia Archive Office', 'Kyiv, 1 Arkhivna Street', FALSE, 'Inactive dev office used to verify status filters.', 'Inactive location for dev status filters.', 'https://maps.google.com/?q=Kyiv+1+Arkhivna+Street'),
        ('Ataraksia Podil Room', 'Kyiv, 14 Podilska Street', TRUE, 'Quiet room for evening appointments. Dev extra office.', 'Ring the Podil room bell and wait near the reception desk.', 'https://maps.google.com/?q=Kyiv+14+Podilska+Street'),
        ('Ataraksia Lviv Pop-up', 'Lviv, 7 Rynok Square', TRUE, 'Temporary pop-up location used for cross-office filters.', 'Use the side entrance from the square.', 'https://maps.google.com/?q=Lviv+7+Rynok+Square'),
        ('Ataraksia Maintenance Room', 'Kyiv, 99 Service Lane', FALSE, 'Inactive maintenance room for negative booking checks.', 'Inactive maintenance location.', 'https://maps.google.com/?q=Kyiv+99+Service+Lane')
)
INSERT INTO offices (name, address, active, location_details, directions, google_maps_url, created_at, updated_at)
SELECT seed.name, seed.address, seed.active, seed.location_details, seed.directions, seed.google_maps_url, NOW(), NOW()
FROM seed
WHERE NOT EXISTS (
    SELECT 1
    FROM offices office
    WHERE office.name = seed.name
);
