-- Sépare le nom affiché de la clé d'unicité pour les entités hydrographiques
-- (#134). Les toponymes BD TOPO ne sont pas uniques (« Lac Blanc » désigne 17
-- plans d'eau) : water_entity.name portait jusqu'ici une contrainte UNIQUE,
-- ce qui forçait l'import à suffixer le nom lui-même pour départager les
-- homonymes (« Lac Blanc (Valloire) », voire « Lac Blanc_PLANDEAU... » quand
-- même la commune ne suffisait pas) — un libellé technique montré tel quel
-- au pêcheur.
--
-- Désormais :
--   - name        : toponyme brut, JAMAIS suffixé, non unique. C'est ce que
--                    voit le pêcheur ; les homonymes se distinguent par la
--                    commune affichée à côté (#6/#15), pas par le nom lui-même.
--   - export_as   : reste UNIQUE et garde l'escalier de désambiguïsation
--                    (toponyme -> toponyme (commune) -> toponyme_cleabs).
--                    Jamais montré au pêcheur ; utilisé pour les exports
--                    Darwin Core, où l'unicité du libellé importe.
--
-- scripts/import_hydro_gpkg.sql est mis à jour en conséquence. Les entités
-- déjà suffixées dans `name` retrouvent un toponyme brut au prochain réimport
-- (pas de rattrapage de données ici : dériver le toponyme brut à partir d'un
-- nom déjà suffixé en base serait imprécis, l'import le fait proprement
-- depuis la source BD TOPO).
ALTER TABLE public.water_entity DROP CONSTRAINT water_entity_name_key;

COMMENT ON COLUMN public.water_entity.name IS 'Nom affiché au pêcheur = toponyme BD TOPO brut, non unique (#134)';
COMMENT ON COLUMN public.water_entity.export_as IS 'Libellé stable et unique (exports Darwin Core), avec escalier de désambiguïsation (#134)';
