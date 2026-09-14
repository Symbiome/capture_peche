-- Transforme la table de staging bdtopo_raw.departement (couche « departement »
-- de la BD TOPO IGN, chargée par ogr2ogr depuis le GeoParquet) vers la table
-- applicative departement.
--
-- Idempotent : réexécutable à chaque rafraîchissement du millésime, via un
-- upsert sur code (code INSEE, clé naturelle stable).
--
-- Colonnes source (GeoParquet BD TOPO, couche departement) : code_insee,
-- nom_officiel, cleabs. ogr2ogr met les noms de colonnes en minuscules au
-- staging et nomme la géométrie « geom » (option -lco GEOMETRY_NAME=geom).

INSERT INTO departement (code, name, bdtopo_cleabs, geom)
SELECT
    code_insee,
    nom_officiel,
    cleabs,
    ST_Multi(ST_Force2D(geom))
FROM bdtopo_raw.departement
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    bdtopo_cleabs = EXCLUDED.bdtopo_cleabs,
    geom = EXCLUDED.geom;
