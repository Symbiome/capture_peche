-- Fixture de recette pour les tests backend (#67).
--
-- Migration Flyway RÉPÉTABLE présente UNIQUEMENT dans le classpath de test
-- (src/test/resources) : elle n'est jamais embarquée en dev/prod. Elle amorce le
-- minimum reproductible attendu par les suites REST : un admin national nominatif,
-- un pêcheur nominatif, le référentiel de base et quelques entités hydrographiques.
--
-- Tout est idempotent (WHERE NOT EXISTS) : re-jouable sans doublon.
-- Mots de passe : bcrypt (at.favre). « whatever » pour l'admin, « sispea » pour le pêcheur.

-- 1) Admin national (amorel) — utilisé par loginAsAdmin() / AbstractFisholaTest.
INSERT INTO public.fishola_admin (email, password, created_on, can_create_admin, is_national_admin, is_operator)
SELECT 'amorel@codelutin.com',
       '$2a$10$8oOr.XUVf3zoMXFDg.gdJuJHf36LD9VZQ9Diwbzc7jd7BxalYHOey',
       now(), true, true, false
WHERE NOT EXISTS (SELECT 1 FROM public.fishola_admin WHERE email = 'amorel@codelutin.com');

-- 2) Pêcheur (thimel) — utilisé par login("thimel@codelutin.com", "sispea").
INSERT INTO public.fishola_user (first_name, last_name, email, password, created_on, pseudo)
SELECT 'Thimel', 'Recette', 'thimel@codelutin.com',
       '$2a$10$j3eTBbEJO9IutgxGIJFMx.uauNH6Z4UR/UeiL62eL5oYMNCSPWYS.',
       now(), 'thimel'
WHERE NOT EXISTS (SELECT 1 FROM public.fishola_user WHERE email = 'thimel@codelutin.com');

-- 2b) Pêcheur dédié à LicenceDaoTest (supprimé puis recréé par le test).
INSERT INTO public.fishola_user (first_name, last_name, email, password, created_on, pseudo)
SELECT 'Chloé', 'Goulon', 'chloe.goulon@inrae.fr',
       '$2a$10$j3eTBbEJO9IutgxGIJFMx.uauNH6Z4UR/UeiL62eL5oYMNCSPWYS.',
       now(), 'chloe'
WHERE NOT EXISTS (SELECT 1 FROM public.fishola_user WHERE email = 'chloe.goulon@inrae.fr');

-- 3) Référentiel de base (aligné sur le seed de recette du back-office).
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

INSERT INTO public.released_fish_state (name, export_as)
SELECT * FROM (VALUES
    ('Vif et vigoureux', 'Vif et vigoureux'),
    ('Affaibli',         'Affaibli'),
    ('Blessé',           'Blessé'),
    ('Mort au relâcher', 'Mort au relâcher')
) v(name, export_as)
WHERE NOT EXISTS (SELECT 1 FROM public.released_fish_state);

-- 4) Entités hydrographiques (avec géométrie ponctuelle ; lat/long sont générées).
-- Les noms courts « Annecy / Bourget / Léman / Aiguebelette » sont attendus par
-- ReferentialResourceTest (name ET exportAs).
-- department (#154) : normalement dérivé par jointure spatiale avec commune ;
-- posé en dur ici, la fixture ne charge pas le référentiel communal.
INSERT INTO public.water_entity (name, export_as, water_entity_code, kind, department, geom)
SELECT v.name, v.name, v.code, v.kind::public.water_entity_kind, v.dep,
       public.ST_SetSRID(public.ST_MakePoint(v.lng, v.lat), 4326)
FROM (VALUES
    ('Annecy',        'LACA', 'STILL',   '74', 6.17, 45.85),
    ('Bourget',       'LACB', 'STILL',   '73', 5.87, 45.72),
    ('Léman',         'LACL', 'STILL',   '74', 6.50, 46.45),
    ('Aiguebelette',  'LACG', 'STILL',   '73', 5.80, 45.55),
    ('Rhône amont',   'RIVR', 'FLOWING', '01', 5.90, 45.80)
) v(name, code, kind, dep, lng, lat)
WHERE NOT EXISTS (SELECT 1 FROM public.water_entity);

-- Une taille réglementaire complète (min + max + maillage) pour couvrir la
-- lecture/écriture de mesh_size (#154).
INSERT INTO public.authorized_sample (water_entity_id, species_id, min_size, max_size, mesh_size)
SELECT we.id, sp.id, 30, 60, 10
FROM public.water_entity we, public.species sp
WHERE we.name = 'Annecy' AND sp.name = 'Carpe commune'
  AND NOT EXISTS (SELECT 1 FROM public.authorized_sample);

-- 5) Contours départementaux (#159) : boîtes englobant les points des entités
-- hydro ci-dessus, pour que la jointure spatiale trip/catch <-> departement.geom
-- résolve en test (la table departement de V1.11.0 n'est pas chargée en dev/test).
--   74 : Annecy (6.17/45.85), Léman (6.50/46.45)
--   73 : Bourget (5.87/45.72), Aiguebelette (5.80/45.55)
--   01 : Rhône amont (5.90/45.80)
INSERT INTO public.departement (code, name, geom)
SELECT v.code, v.name,
       public.ST_Multi(public.ST_GeomFromText(v.wkt, 4326))
FROM (VALUES
    ('74', 'Haute-Savoie', 'POLYGON((6.0 45.7, 7.0 45.7, 7.0 46.6, 6.0 46.6, 6.0 45.7))'),
    ('73', 'Savoie',       'POLYGON((5.6 45.4, 6.0 45.4, 6.0 45.75, 5.6 45.75, 5.6 45.4))'),
    ('01', 'Ain',          'POLYGON((5.6 45.75, 6.0 45.75, 6.0 46.2, 5.6 46.2, 5.6 45.75))')
) v(code, name, wkt)
WHERE NOT EXISTS (SELECT 1 FROM public.departement);
