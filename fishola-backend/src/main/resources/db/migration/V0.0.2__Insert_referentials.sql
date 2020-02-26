INSERT INTO lake (name, latitude, longitude) VALUES
    ('Lac d''Annecy', 45.787, 6.147),
    ('Léman', 46.426, 6.5499),
    ('Lac du Bourget', 45.7249, 5.8684),
    ('Lac d''Aiguebelette', 45.5508, 5.8015);

INSERT INTO species (name, built_in) VALUES
    ('Omble chevalier', true),
    ('Corégone', true),
    ('Brochet', true),
    ('Perche', true),
    ('Truite', true);

INSERT INTO weather (name) VALUES
    ('Ensoleillé'),
    ('Couvert'),
    ('Neigeux'),
    ('Nuageux'),
    ('Orageux'),
    ('Pluvieux'),
    ('Brumeux');

INSERT INTO technique (name, built_in) VALUES
    ('Pêche à la traîne', true),
    ('Pêche à la sonde', true),
    ('Pêche au leurre', true),
    ('Pêche au vif', true),
    ('Pêche au coup', true);

INSERT INTO released_fish_state (name) VALUES
    ('Comme neuf'),
    ('Frais comme un gardon'),
    ('Légèrement blessé'),
    ('Blessé'),
    ('Mort');
