-- ─────────────────────────────────────────────────────────────────────────────
-- FIFA World Cup 2026 — Venue Seed Data
-- image_url stores the static folder path; images are served from that folder
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO venues (id, name, city, country, flag, capacity, lat, lng, image_url, country_color) VALUES

-- USA
(1,  'MetLife Stadium',         'New York / New Jersey', 'USA',    '🇺🇸', 82500,  40.8136, -74.0744,  '/images/USA/MetLife',   '#B22234'),
(2,  'SoFi Stadium',            'Los Angeles',           'USA',    '🇺🇸', 70240,  33.9535, -118.3392, '/images/USA/SoFi',      '#B22234'),
(3,  'AT&T Stadium',            'Dallas',                'USA',    '🇺🇸', 80000,  32.7473, -97.0945,  '/images/USA/AT&T',      '#B22234'),
(4,  'Levi''s Stadium',         'San Francisco Bay Area','USA',    '🇺🇸', 68500,  37.4032, -121.9698, '/images/USA/Levi',      '#B22234'),
(5,  'Hard Rock Stadium',       'Miami',                 'USA',    '🇺🇸', 65326,  25.9580, -80.2389,  '/images/USA/HardRock',  '#B22234'),
(6,  'Gillette Stadium',        'Boston',                'USA',    '🇺🇸', 65878,  42.0909, -71.2643,  '/images/USA/Gillette',  '#B22234'),
(7,  'Lincoln Financial Field', 'Philadelphia',          'USA',    '🇺🇸', 69796,  39.9008, -75.1675,  '/images/USA/Lincoln',   '#B22234'),
(8,  'Arrowhead Stadium',       'Kansas City',           'USA',    '🇺🇸', 76416,  39.0489, -94.4839,  '/images/USA/Arrowhead', '#B22234'),
(9,  'Lumen Field',             'Seattle',               'USA',    '🇺🇸', 68740,  47.5952, -122.3316, '/images/USA/Lumen',     '#B22234'),
(15, 'Mercedes-Benz Stadium',   'Atlanta',               'USA',    '🇺🇸', 71000,  33.7553, -84.4006,  '/images/USA/MercedesBenz', '#B22234'),
(16, 'NRG Stadium',             'Houston',               'USA',    '🇺🇸', 72220,  29.6847, -95.4107,  '/images/USA/NRG',       '#B22234'),

-- Canada
(10, 'BC Place',                'Vancouver',             'Canada', '🇨🇦', 54500,  49.2768, -123.1115, '/images/Canada/bcplace',  '#FF0000'),
(11, 'BMO Field',               'Toronto',               'Canada', '🇨🇦', 45736,  43.6333, -79.4187,  '/images/Canada/bmofield', '#FF0000'),

-- Mexico
(12, 'Estadio Azteca',          'Mexico City',           'Mexico', '🇲🇽', 87523,  19.3029, -99.1505,  '/images/Mexico/azteca', '#006847'),
(13, 'Estadio Akron',           'Guadalajara',           'Mexico', '🇲🇽', 45456,  20.6867, -103.4670, '/images/Mexico/akron',  '#006847'),
(14, 'Estadio BBVA',            'Monterrey',             'Mexico', '🇲🇽', 53500,  25.6693, -100.2440, '/images/Mexico/BBVA',   '#006847')

ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url;
