-- Transforme la table de staging bdtopo_raw.commune (couche COMMUNE d'ADMIN
-- EXPRESS, chargée par ogr2ogr depuis le GeoPackage) vers la table applicative
-- commune.
--
-- Idempotent : réexécutable à chaque rafraîchissement du millésime, via un
-- upsert sur insee_com (code INSEE, clé naturelle stable).
--
-- Colonnes source attendues (ADMIN EXPRESS COG, couche COMMUNE) : insee_com, nom.
-- ogr2ogr met les noms de colonnes en minuscules au staging.

INSERT INTO commune (insee_com, name, geom)
SELECT
    insee_com,
    nom,
    ST_Multi(ST_Force2D(geom))
FROM bdtopo_raw.commune
ON CONFLICT (insee_com) DO UPDATE SET
    name = EXCLUDED.name,
    geom = EXCLUDED.geom;
