-- Configuration des tailles réglementaires par espèce et par milieu (#154).
--
-- 1. authorized_sample gagne un maillage distinct de la taille minimale.
--    Le back-office « Maillages et tailles maximales » raisonnait jusqu'ici en
--    deux valeurs (min_size / max_size), la colonne min_size servant à la fois
--    de « taille minimale légale » et de « maillage ». Ce sont deux notions
--    réglementaires différentes : min_size reste la taille minimale de capture
--    (obligatoire dès qu'une espèce est réglementée), max_size et mesh_size
--    sont facultatifs.
--
-- 2. water_entity gagne un code département, pour borner le périmètre du
--    back-office AVANT de construire la matrice espèces × entités. La table
--    compte ~181 000 lignes (extension multi-milieux RM&C, #134) et grandira
--    au chargement national (#51) : charger l'intégralité du référentiel au
--    montage de la page faisait tomber le backend en OutOfMemoryError. Le
--    département est dérivé par jointure spatiale avec `commune` (code INSEE) ;
--    scripts/import_hydro_gpkg.sql fait le même calcul à chaque réimport.

ALTER TABLE public.authorized_sample ADD COLUMN mesh_size integer;

COMMENT ON COLUMN public.authorized_sample.min_size IS 'Taille minimale légale de capture, en cm (0 = non définie)';
COMMENT ON COLUMN public.authorized_sample.max_size IS 'Taille maximale de capture, en cm (1000 = non définie)';
COMMENT ON COLUMN public.authorized_sample.mesh_size IS 'Maillage réglementaire, en cm (NULL = non défini) — #154';

ALTER TABLE public.water_entity ADD COLUMN department character varying(3);

COMMENT ON COLUMN public.water_entity.department IS 'Code département INSEE, dérivé par jointure spatiale avec commune ; filtre de périmètre du back-office (#154)';

-- Rattrapage des données déjà en base : commune de plus grand recouvrement
-- (surface pour un plan d'eau, longueur pour un cours d'eau), NULL si le
-- référentiel commune ne couvre pas encore la zone — le rattachement se fera
-- alors au prochain import hydro, communes chargées.
UPDATE public.water_entity w
SET department = located.department
FROM (
    SELECT we.id,
           (SELECT CASE
                       WHEN c.insee_com LIKE '97%' OR c.insee_com LIKE '98%'
                           THEN substring(c.insee_com, 1, 3)
                       ELSE substring(c.insee_com, 1, 2)
                   END
              FROM public.commune c
             WHERE public.ST_Intersects(c.geom, we.geom)
             ORDER BY public.ST_Area(public.ST_Intersection(c.geom, we.geom)) DESC,
                      public.ST_Length(public.ST_Intersection(c.geom, we.geom)) DESC,
                      c.insee_com
             LIMIT 1) AS department
    FROM public.water_entity we
) located
WHERE located.id = w.id
  AND located.department IS NOT NULL;

CREATE INDEX water_entity_department_idx ON public.water_entity (department);
