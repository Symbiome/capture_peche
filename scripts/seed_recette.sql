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

-- ── Entités hydrographiques de recette ─────────────────────────────────────────
-- « Lac A (recette) » = dans le périmètre régional/opérateur ; « Lac B (recette) » = hors périmètre.
INSERT INTO public.water_entity (name, export_as, water_entity_code, kind, geom)
SELECT v.name, v.name, v.code, v.kind::public.water_entity_kind,
       public.ST_SetSRID(public.ST_MakePoint(v.lng, v.lat), 4326)
FROM (VALUES
    ('Lac A (recette)', 'RECA', 'STILL', 6.17, 45.85),
    ('Lac B (recette)', 'RECB', 'STILL', 5.87, 45.72)
) v(name, code, kind, lng, lat)
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

-- ── Périmètres (régional + opérateur → « Lac A (recette) ») ─────────────────────
INSERT INTO public.fishola_admin_water_entities (fishola_admin_id, water_entity_id)
SELECT a.id, w.id
FROM public.fishola_admin a
JOIN public.water_entity w ON w.name = 'Lac A (recette)'
WHERE a.email IN ('regional.recette@fishola.test', 'operateur.recette@fishola.test')
  AND NOT EXISTS (
      SELECT 1 FROM public.fishola_admin_water_entities l
      WHERE l.fishola_admin_id = a.id AND l.water_entity_id = w.id
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
