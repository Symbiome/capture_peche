-- Seed de référentiel pour la RECETTE / DEV uniquement (#recette).
-- Les migrations sont schema-only (pg_dump) : espèces, techniques, météo et
-- états de relâche sont donc vides sur une base fraîche, ce qui bloque la saisie
-- d'une sortie (menus vides). Ce script pose un jeu minimal réaliste (eau douce
-- AURA / lacs alpins). Idempotent : n'insère que si la table cible est vide.
-- NB : ce n'est PAS de la donnée métier officielle — à remplacer par le vrai
-- référentiel fourni par l'UFBRMC pour la V1.

-- Espèces (built_in, taille obligatoire par défaut).
INSERT INTO public.species (name, export_as, built_in, mandatory_size)
SELECT * FROM (VALUES
    ('Truite fario',        'Truite fario',        true, true),
    ('Truite arc-en-ciel',  'Truite arc-en-ciel',  true, true),
    ('Omble chevalier',     'Omble chevalier',     true, true),
    ('Corégone (féra)',     'Corégone',            true, true),
    ('Ombre commun',        'Ombre commun',        true, true),
    ('Brochet',             'Brochet',             true, true),
    ('Perche',              'Perche',              true, true),
    ('Sandre',              'Sandre',              true, true),
    ('Black-bass',          'Black-bass',          true, true),
    ('Gardon',              'Gardon',              true, false),
    ('Brème commune',       'Brème commune',       true, false),
    ('Carpe commune',       'Carpe commune',       true, true),
    ('Tanche',              'Tanche',              true, false),
    ('Silure glane',        'Silure glane',        true, true)
) v(name, export_as, built_in, mandatory_size)
WHERE NOT EXISTS (SELECT 1 FROM public.species);

-- Techniques de pêche (built_in).
INSERT INTO public.technique (name, export_as, built_in)
SELECT * FROM (VALUES
    ('Pêche aux leurres',   'Pêche aux leurres',   true),
    ('Pêche à la mouche',   'Pêche à la mouche',   true),
    ('Pêche au coup',       'Pêche au coup',       true),
    ('Pêche au toc',        'Pêche au toc',        true),
    ('Pêche au vif / poser','Pêche au vif',        true),
    ('Pêche à la traîne',   'Pêche à la traîne',   true),
    ('Pêche au feeder',     'Pêche au feeder',     true),
    ('Pêche à la bouée',    'Pêche à la bouée',    true)
) v(name, export_as, built_in)
WHERE NOT EXISTS (SELECT 1 FROM public.technique);

-- Conditions météo.
INSERT INTO public.weather (name, export_as)
SELECT * FROM (VALUES
    ('Ensoleillé',  'Ensoleillé'),
    ('Peu nuageux', 'Peu nuageux'),
    ('Couvert',     'Couvert'),
    ('Pluvieux',    'Pluvieux'),
    ('Orageux',     'Orageux'),
    ('Brouillard',  'Brouillard'),
    ('Venteux',     'Venteux')
) v(name, export_as)
WHERE NOT EXISTS (SELECT 1 FROM public.weather);

-- États du poisson relâché.
INSERT INTO public.released_fish_state (name, export_as)
SELECT * FROM (VALUES
    ('Vif et vigoureux', 'Vif et vigoureux'),
    ('Affaibli',         'Affaibli'),
    ('Blessé',           'Blessé'),
    ('Mort au relâcher', 'Mort au relâcher')
) v(name, export_as)
WHERE NOT EXISTS (SELECT 1 FROM public.released_fish_state);
