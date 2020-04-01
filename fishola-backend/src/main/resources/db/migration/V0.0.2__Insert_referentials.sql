INSERT INTO lake (name, latitude, longitude, export_as) VALUES
    ('Lac d''Annecy', 45.787, 6.147, 'Annecy'),
    ('Léman', 46.426, 6.5499, 'Léman'),
    ('Lac du Bourget', 45.7249, 5.8684, 'Bourget'),
    ('Lac d''Aiguebelette', 45.5508, 5.8015, 'Aiguebelette');

INSERT INTO species (name, built_in, export_as) VALUES
    ('Omble chevalier', true, 'omble chevalier'),
    ('Corégone', true, 'coregone'),
    ('Brochet', true, 'brochet'),
    ('Perche', true, 'perche'),
    ('Truite', true, 'truite');

INSERT INTO weather (name, export_as) VALUES
    ('Ensoleillé', 'Ensoleillé'),
    ('Couvert', 'Couvert'),
    ('Neigeux', 'Neigeux'),
    ('Nuageux', 'Nuageux'),
    ('Orageux', 'Orageux'),
    ('Pluvieux', 'Pluvieux'),
    ('Brumeux', 'Brumeux');

INSERT INTO technique (name, built_in, export_as) VALUES
    ('Pêche à la traîne', true, 'traine'),
    ('Pêche à la sonde', true, 'sonde'),
    ('Pêche au leurre', true, 'leurre'),
    ('Pêche au vif', true, 'vif'),
    ('Pêche au coup', true, 'coup');

INSERT INTO released_fish_state (name, export_as) VALUES
    ('Comme neuf', 'Comme neuf'),
    ('Frais comme un gardon', 'Frais comme un gardon'),
    ('Légèrement blessé', 'Légèrement blessé'),
    ('Blessé', 'Blessé'),
    ('Mort', 'Mort');

INSERT INTO sample_type (name, export_as) VALUES
    ('Écailles', 'ecailles'),
    ('Estomac', 'estomac'),
    ('Tête', 'tete');
