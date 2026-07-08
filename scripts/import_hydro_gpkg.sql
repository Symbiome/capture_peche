-- Transforme les tables de staging bdtopo_raw.* (chargées par ogr2ogr depuis les
-- GeoPackages BD TOPO) vers les tables applicatives river_section / water_surface.
--
-- Idempotent : réexécutable à chaque rafraîchissement des données source, via un
-- upsert sur bdtopo_cleabs (identifiant national BD TOPO, clé naturelle stable).

INSERT INTO river_section (bdtopo_cleabs, persistent, width_class, flow_direction, geom)
SELECT
    cleabs,
    CASE persistance
        WHEN 'Permanent' THEN true
        WHEN 'Intermittent' THEN false
        ELSE NULL
    END AS persistent,
    classe_de_largeur,
    sens_de_l_ecoulement,
    ST_Force2D(geom)
FROM bdtopo_raw.troncon_hydrographique
ON CONFLICT (bdtopo_cleabs) DO UPDATE SET
    persistent = EXCLUDED.persistent,
    width_class = EXCLUDED.width_class,
    flow_direction = EXCLUDED.flow_direction,
    geom = EXCLUDED.geom;

INSERT INTO water_surface (bdtopo_cleabs, nature, geom)
SELECT
    cleabs,
    nature,
    ST_Multi(ST_Force2D(geom))
FROM bdtopo_raw.surface_hydrographique
ON CONFLICT (bdtopo_cleabs) DO UPDATE SET
    nature = EXCLUDED.nature,
    geom = EXCLUDED.geom;
