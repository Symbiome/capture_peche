
-- Corégone, Truite & Omble chevalier sur tous les lacs
INSERT INTO authorized_sample (lake_id, species_id)
    SELECT l.id AS lake_id, s.id AS species_id
    FROM lake l, species s
    WHERE s.name in ('Corégone', 'Truite', 'Omble chevalier');
