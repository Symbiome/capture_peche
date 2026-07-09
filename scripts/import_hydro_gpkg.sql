-- Transforme les tables de staging bdtopo_raw.* (chargées par ogr2ogr depuis les
-- GeoPackages BD TOPO) vers les tables applicatives water_entity / river_section /
-- water_surface.
--
-- Idempotent : réexécutable à chaque rafraîchissement des données source, via un
-- upsert sur bdtopo_cleabs (identifiant national BD TOPO, clé naturelle stable).
--
-- Ordre : water_entity (plan_d_eau, cours_d_eau) est importé en premier, pour que
-- river_section / water_surface puissent résoudre leur water_entity_id directement
-- à l'insertion (jointure sur bdtopo_cleabs), sans passe de backfill séparée.

-- ---------------------------------------------------------------------------
-- 1. plan_d_eau -> water_entity (kind = STILL)
-- ---------------------------------------------------------------------------

WITH ranked AS (
    SELECT *,
           row_number() OVER (PARTITION BY coalesce(nullif(toponyme, ''), cleabs) ORDER BY cleabs) AS rn
    FROM bdtopo_raw.plan_d_eau
)
INSERT INTO water_entity (name, export_as, kind, nature, altitude_moyenne, bdtopo_cleabs, geom)
SELECT
    CASE WHEN rn = 1 THEN coalesce(nullif(toponyme, ''), cleabs)
         ELSE coalesce(nullif(toponyme, ''), cleabs) || '_' || cleabs END,
    CASE WHEN rn = 1 THEN coalesce(nullif(toponyme, ''), cleabs)
         ELSE coalesce(nullif(toponyme, ''), cleabs) || '_' || cleabs END,
    'STILL'::water_entity_kind,
    nature,
    altitude_moyenne,
    cleabs,
    ST_Force2D(geom)
FROM ranked
ON CONFLICT (bdtopo_cleabs) DO UPDATE SET
    name = EXCLUDED.name,
    export_as = EXCLUDED.export_as,
    nature = EXCLUDED.nature,
    altitude_moyenne = EXCLUDED.altitude_moyenne,
    geom = EXCLUDED.geom;

-- ---------------------------------------------------------------------------
-- 2. cours_d_eau -> water_entity (kind = FLOWING)
-- ---------------------------------------------------------------------------

WITH ranked AS (
    SELECT *,
           row_number() OVER (PARTITION BY coalesce(nullif(toponyme, ''), cleabs) ORDER BY cleabs) AS rn
    FROM bdtopo_raw.cours_d_eau
)
INSERT INTO water_entity (name, export_as, kind, bdtopo_cleabs, geom)
SELECT
    CASE WHEN rn = 1 THEN coalesce(nullif(toponyme, ''), cleabs)
         ELSE coalesce(nullif(toponyme, ''), cleabs) || '_' || cleabs END,
    CASE WHEN rn = 1 THEN coalesce(nullif(toponyme, ''), cleabs)
         ELSE coalesce(nullif(toponyme, ''), cleabs) || '_' || cleabs END,
    'FLOWING'::water_entity_kind,
    cleabs,
    ST_Force2D(geom)
FROM ranked
ON CONFLICT (bdtopo_cleabs) DO UPDATE SET
    name = EXCLUDED.name,
    export_as = EXCLUDED.export_as,
    geom = EXCLUDED.geom;

-- ---------------------------------------------------------------------------
-- 3. troncon_hydrographique -> river_section (water_entity_id resolved via
--    liens_vers_cours_d_eau -> water_entity.bdtopo_cleabs)
-- ---------------------------------------------------------------------------

INSERT INTO river_section (water_entity_id, bdtopo_cleabs, persistent, width_class, flow_direction, geom)
SELECT
    we.id,
    t.cleabs,
    CASE t.persistance
        WHEN 'Permanent' THEN true
        WHEN 'Intermittent' THEN false
        ELSE NULL
    END,
    t.classe_de_largeur,
    t.sens_de_l_ecoulement,
    ST_Force2D(t.geom)
FROM bdtopo_raw.troncon_hydrographique t
LEFT JOIN water_entity we ON we.bdtopo_cleabs = t.liens_vers_cours_d_eau
ON CONFLICT (bdtopo_cleabs) DO UPDATE SET
    water_entity_id = EXCLUDED.water_entity_id,
    persistent = EXCLUDED.persistent,
    width_class = EXCLUDED.width_class,
    flow_direction = EXCLUDED.flow_direction,
    geom = EXCLUDED.geom;

-- ---------------------------------------------------------------------------
-- 4. surface_hydrographique -> water_surface (water_entity_id resolved via
--    liens_vers_plan_d_eau or liens_vers_cours_d_eau -> water_entity.bdtopo_cleabs)
-- ---------------------------------------------------------------------------

INSERT INTO water_surface (water_entity_id, bdtopo_cleabs, nature, geom)
SELECT
    we.id,
    s.cleabs,
    s.nature,
    ST_Multi(ST_Force2D(s.geom))
FROM bdtopo_raw.surface_hydrographique s
LEFT JOIN water_entity we
    ON we.bdtopo_cleabs = coalesce(nullif(s.liens_vers_plan_d_eau, ''), nullif(s.liens_vers_cours_d_eau, ''))
ON CONFLICT (bdtopo_cleabs) DO UPDATE SET
    water_entity_id = EXCLUDED.water_entity_id,
    nature = EXCLUDED.nature,
    geom = EXCLUDED.geom;
