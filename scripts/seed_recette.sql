-- Seed RUNTIME de recette (local) — provisionne la stack de dev pour la recette manuelle.
--
-- NE PAS confondre avec la fixture de TEST (src/test/resources/.../R__test_fixture.sql,
-- réservée aux TU). Ce script s'applique à la base de dev « fishola » :
--
--   docker exec -i postgres-18-fishola psql -U postgres -d fishola < scripts/seed_recette.sql
--   (ou : ./scripts/seed_recette.sh)
--
-- Idempotent (WHERE NOT EXISTS) : rejouable sans doublon.
-- Comptes staff + pêcheur : mot de passe « Recette2026! » (bcrypt).

-- ── Référentiel de base ────────────────────────────────────────────────────────
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

-- ── Contours départementaux de recette (#159) ─────────────────────────────────
-- Boîtes englobant les lacs ci-dessous, pour que l'estampillage spatial
-- trip/catch <-> departement.geom fonctionne sans charger la BD TOPO complète.
INSERT INTO public.departement (code, name, geom)
SELECT v.code, v.name, public.ST_Multi(public.ST_GeomFromText(v.wkt, 4326))
FROM (VALUES
    ('74', 'Haute-Savoie', 'POLYGON((6.0 45.7, 7.0 45.7, 7.0 46.6, 6.0 46.6, 6.0 45.7))'),
    ('73', 'Savoie',       'POLYGON((5.6 45.4, 6.0 45.4, 6.0 45.9, 5.6 45.9, 5.6 45.4))')
) v(code, name, wkt)
WHERE NOT EXISTS (SELECT 1 FROM public.departement WHERE code IN ('73', '74'));

-- ── Entités hydrographiques de recette ─────────────────────────────────────────
-- « Lac A (recette) » (dép. 74) = dans le périmètre régional/opérateur ;
-- « Lac B (recette) » (dép. 73) = hors périmètre.
INSERT INTO public.water_entity (name, export_as, water_entity_code, kind, department, geom)
SELECT v.name, v.name, v.code, v.kind::public.water_entity_kind, v.dep,
       public.ST_SetSRID(public.ST_MakePoint(v.lng, v.lat), 4326)
FROM (VALUES
    ('Lac A (recette)', 'RECA', 'STILL', '74', 6.17, 45.85),
    ('Lac B (recette)', 'RECB', 'STILL', '73', 5.87, 45.72)
) v(name, code, kind, dep, lng, lat)
WHERE NOT EXISTS (SELECT 1 FROM public.water_entity WHERE name IN ('Lac A (recette)', 'Lac B (recette)'));

-- ── Comptes staff (mot de passe « Recette2026! ») ──────────────────────────────
-- Admin national.
INSERT INTO public.fishola_admin (email, password, created_on, can_create_admin, is_national_admin, is_operator)
SELECT 'national.recette@fishola.test',
       '$2a$10$q.VM4UGUlkuqULFZPoxJxOUf9YhQ/gxSAZ1xcxhts/FZlx52YTzSO',
       now(), true, true, false
WHERE NOT EXISTS (SELECT 1 FROM public.fishola_admin WHERE email = 'national.recette@fishola.test');

-- Admin régional (peut gérer, mais borné à son périmètre).
INSERT INTO public.fishola_admin (email, password, created_on, can_create_admin, is_national_admin, is_operator)
SELECT 'regional.recette@fishola.test',
       '$2a$10$q.VM4UGUlkuqULFZPoxJxOUf9YhQ/gxSAZ1xcxhts/FZlx52YTzSO',
       now(), true, false, false
WHERE NOT EXISTS (SELECT 1 FROM public.fishola_admin WHERE email = 'regional.recette@fishola.test');

-- Opérateur (saisie / import, cantonné à son périmètre).
INSERT INTO public.fishola_admin (email, password, created_on, can_create_admin, is_national_admin, is_operator)
SELECT 'operateur.recette@fishola.test',
       '$2a$10$q.VM4UGUlkuqULFZPoxJxOUf9YhQ/gxSAZ1xcxhts/FZlx52YTzSO',
       now(), false, false, true
WHERE NOT EXISTS (SELECT 1 FROM public.fishola_admin WHERE email = 'operateur.recette@fishola.test');

-- ── Pêcheur (app mobile) ───────────────────────────────────────────────────────
INSERT INTO public.fishola_user (first_name, last_name, email, password, created_on, pseudo)
SELECT 'Pêcheur', 'Recette', 'pecheur.recette@fishola.test',
       '$2a$10$q.VM4UGUlkuqULFZPoxJxOUf9YhQ/gxSAZ1xcxhts/FZlx52YTzSO',
       now(), 'pecheur.recette'
WHERE NOT EXISTS (SELECT 1 FROM public.fishola_user WHERE email = 'pecheur.recette@fishola.test');

-- ── Périmètres départementaux (#159) : régional + opérateur → dép. 74 ──────────
-- Lac A (recette) est dans le 74 (périmètre), Lac B (recette) dans le 73 (hors).
INSERT INTO public.fishola_admin_departments (fishola_admin_id, department_code)
SELECT a.id, '74'
FROM public.fishola_admin a
WHERE a.email IN ('regional.recette@fishola.test', 'operateur.recette@fishola.test')
  AND NOT EXISTS (
      SELECT 1 FROM public.fishola_admin_departments d
      WHERE d.fishola_admin_id = a.id AND d.department_code = '74'
  );

-- ── Seuils de tailles aberrantes (règle métier Q8) ─────────────────────────────
-- Sans ces bornes, l'étape « règle métier » de l'import ne rejette jamais rien :
-- les seuils officiels relèvent du provisionnement du référentiel.
INSERT INTO public.species_size_bounds (species_id, min_size_cm, max_size_cm)
SELECT s.id, v.mini, v.maxi
FROM (VALUES
    ('Perche',       5,  60),
    ('Brochet',     20, 140),
    ('Truite fario', 10,  90)
) v(nom, mini, maxi)
JOIN public.species s ON s.name = v.nom
WHERE NOT EXISTS (SELECT 1 FROM public.species_size_bounds b WHERE b.species_id = s.id);
