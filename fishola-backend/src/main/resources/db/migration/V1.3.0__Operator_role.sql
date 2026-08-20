-- #69 — Rôle « opérateur » (agent de fédération) : saisie / import de masse (CdC §3.1.5.3).
--
-- Modèle de droits PAR RÔLE, dérivé des drapeaux de `fishola_admin` :
--   - administrateur national  : is_national_admin = true
--   - administrateur régional   : is_national_admin = false et is_operator = false (+ périmètre)
--   - opérateur                 : is_operator = true (+ périmètre)
--
-- Un opérateur N'EST PAS administrateur : il porte is_national_admin = false et
-- can_create_admin = false, et il est cantonné à un périmètre via
-- `fishola_admin_water_entities`, comme un administrateur régional.
--
-- Additif et rétro-compatible : DEFAULT false => toutes les lignes existantes restent
-- des administrateurs (is_operator = false).

ALTER TABLE public.fishola_admin
    ADD COLUMN is_operator boolean DEFAULT false NOT NULL;

COMMENT ON COLUMN public.fishola_admin.is_operator IS
    'Rôle opérateur (agent de fédération, saisie/import §3.1.5.3). Exclusif des rôles admin : un opérateur a is_national_admin = false et can_create_admin = false, et porte un périmètre via fishola_admin_water_entities.';
