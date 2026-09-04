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
--
-- Nommage des plans d'eau (#134) : les toponymes BD TOPO ne sont pas uniques
-- (« Lac Blanc » désigne 17 plans d'eau distincts), alors que
-- water_entity.export_as porte une contrainte UNIQUE (water_entity.name n'en
-- porte plus, cf. #134). `name` (nom affiché au pêcheur) vaut donc TOUJOURS le
-- toponyme brut, jamais suffixé. Seul `export_as` (jamais montré au pêcheur,
-- utilisé pour les exports Darwin Core) suit un escalier de désambiguïsation
-- pour rester unique : toponyme nu → « toponyme (commune) », quand le
-- référentiel `commune` la couvre, → « toponyme_cleabs » sinon. Charger les
-- communes des départements concernés AVANT l'import améliore donc les
-- libellés d'export (scripts/import_communes_geoapi.sh ou import_admin_gpkg.sh) ;
-- sans elles, l'import réussit quand même et export_as retombe sur le suffixe
-- cleabs (name reste le toponyme brut dans tous les cas).
--
-- Les cours d'eau n'utilisent volontairement pas la commune pour leur
-- export_as : une entité cours_d_eau porte le cours entier (l'Arve traverse
-- 26 communes du seul 74), un qualificatif communal y serait faux autant
-- qu'inutile — seul le suffixe cleabs les désambiguïsent au besoin.

-- ---------------------------------------------------------------------------
-- 1. plan_d_eau -> water_entity (kind = STILL)
-- ---------------------------------------------------------------------------

WITH staged AS (
    SELECT *, coalesce(nullif(toponyme, ''), cleabs) AS base_name
    FROM bdtopo_raw.plan_d_eau
), located AS (
    -- Commune de plus grand recouvrement, pour départager les homonymes par un
    -- libellé lisible plutôt que par un identifiant technique. Le recouvrement
    -- maximal (et non le simple ST_Intersects) traite les plans d'eau à cheval
    -- sur plusieurs communes. NULL si le référentiel commune ne couvre pas la
    -- zone : le repli sur cleabs s'applique alors, l'import n'échoue jamais.
    SELECT staged.*,
           (SELECT c.name
              FROM commune c
             WHERE ST_Intersects(c.geom, staged.geom)
             ORDER BY ST_Area(ST_Intersection(c.geom, staged.geom)) DESC
             LIMIT 1) AS commune_name
    FROM staged
), incumbency AS (
    -- Qui détient DÉJÀ le nom qu'il pourrait revendiquer ? Les contraintes UNIQUE
    -- de water_entity sont vérifiées ligne à ligne, pas en fin d'instruction : si
    -- deux plans d'eau du même lot échangent leurs noms, l'état intermédiaire
    -- viole la contrainte et l'import échoue, alors même que l'état final serait
    -- valide. Donner la priorité au détenteur en place supprime tout échange.
    SELECT located.*,
           located.base_name || ' (' || located.commune_name || ')' AS qualified_name,
           EXISTS (
               SELECT 1 FROM water_entity w
               WHERE w.bdtopo_cleabs = located.cleabs
                 AND (w.name = located.base_name OR w.export_as = located.base_name)
           ) AS holds_base,
           EXISTS (
               SELECT 1 FROM water_entity w
               WHERE w.bdtopo_cleabs = located.cleabs
                 AND (w.name = located.base_name || ' (' || located.commune_name || ')'
                   OR w.export_as = located.base_name || ' (' || located.commune_name || ')')
           ) AS holds_qualified
    FROM located
), candidate AS (
    SELECT incumbency.*,
           -- Rang dans le lot, détenteur en place d'abord. À défaut, la cleabs
           -- départage de façon déterministe.
           row_number() OVER (
               PARTITION BY base_name
               ORDER BY (CASE WHEN holds_base THEN 0 ELSE 1 END), cleabs
           ) AS rn,
           -- Unicité du nom qualifié DANS le lot : plusieurs plans d'eau peuvent
           -- porter le même toponyme dans la même commune (chapelets de lacs,
           -- étangs de la Dombes, darses portuaires). Le premier prend le nom
           -- qualifié, les suivants retombent sur cleabs.
           row_number() OVER (
               PARTITION BY base_name, commune_name
               ORDER BY (CASE WHEN holds_qualified THEN 0 ELSE 1 END), cleabs
           ) AS rn_commune
    FROM incumbency
), named AS (
    -- Escalier de désambiguïsation, du plus lisible au plus technique :
    --   1. le toponyme nu, s'il est réellement libre ;
    --   2. « toponyme (commune) », s'il est libre à son tour ;
    --   3. « toponyme_cleabs », toujours unique par construction.
    --
    -- « Libre » se vérifie DANS le lot (row_number) ET en base, contre une AUTRE
    -- entité : sans cette seconde condition, ajouter un département dont un
    -- toponyme existe déjà (« Lac Blanc » en Savoie et en Haute-Savoie) violait
    -- la contrainte UNIQUE sur name / export_as. La comparaison porte aussi sur
    -- export_as, car des lignes hors BD TOPO peuvent porter un nom sans
    -- bdtopo_cleabs. Exclure sa propre cleabs rend le résultat stable au rejeu :
    -- une entité qui détient déjà un nom le conserve.
    --
    -- Les autres membres du lot sont exclus eux aussi (NOT EXISTS sur le staging),
    -- et c'est essentiel à l'idempotence : ils sont sur le point d'être renommés,
    -- donc un nom qu'ils détiennent encore ne doit pas bloquer un rang inférieur.
    -- Sans cette exclusion, deux « Lac Noir » du même lot se cédaient le nom nu
    -- d'une exécution à l'autre — le rang 1 ne pouvait pas le prendre, le rang 2
    -- le libérait, et le rang 1 s'en emparait au rejeu suivant. Le nom ne dépend
    -- désormais que du contenu du lot et des lignes hors lot, tous deux inchangés
    -- par cette instruction : le résultat est donc stable.
    SELECT candidate.*,
           CASE
               WHEN rn = 1 AND NOT EXISTS (
                        SELECT 1
                        FROM water_entity w
                        WHERE (w.name = candidate.base_name OR w.export_as = candidate.base_name)
                          AND w.bdtopo_cleabs IS DISTINCT FROM candidate.cleabs
                          AND NOT EXISTS (
                              SELECT 1 FROM bdtopo_raw.plan_d_eau b
                              WHERE b.cleabs = w.bdtopo_cleabs
                          )
                    ) THEN candidate.base_name
               WHEN candidate.commune_name IS NOT NULL
                    AND rn_commune = 1
                    AND NOT EXISTS (
                        SELECT 1
                        FROM water_entity w
                        WHERE (w.name = candidate.qualified_name OR w.export_as = candidate.qualified_name)
                          AND w.bdtopo_cleabs IS DISTINCT FROM candidate.cleabs
                          AND NOT EXISTS (
                              SELECT 1 FROM bdtopo_raw.plan_d_eau b
                              WHERE b.cleabs = w.bdtopo_cleabs
                          )
                    ) THEN candidate.qualified_name
               ELSE candidate.base_name || '_' || candidate.cleabs
           END AS final_name
    FROM candidate
)
INSERT INTO water_entity (name, export_as, kind, nature, altitude_moyenne, bdtopo_cleabs, geom)
SELECT
    -- name (#134) : toponyme brut, jamais l'escalier — c'est export_as qui
    -- porte la désambiguïsation, name n'a plus de contrainte d'unicité à tenir.
    base_name,
    final_name,
    'STILL'::water_entity_kind,
    nature,
    altitude_moyenne,
    cleabs,
    ST_Force2D(geom)
FROM named
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
           coalesce(nullif(toponyme, ''), cleabs) AS base_name,
           row_number() OVER (PARTITION BY coalesce(nullif(toponyme, ''), cleabs) ORDER BY cleabs) AS rn
    FROM bdtopo_raw.cours_d_eau
), named AS (
    -- Le nom nu n'est retenu que s'il est réellement libre : premier de son groupe
    -- DANS le lot, et pas déjà porté en base par une AUTRE entité. Sans cette
    -- seconde condition, ajouter un département dont un toponyme existe déjà
    -- (« Lac Blanc » en Savoie et en Haute-Savoie, « Lac Vert », « Lac Cornu »...)
    -- violait la contrainte UNIQUE sur name / export_as : la déduplication ne
    -- portait que sur le lot courant. On compare aussi export_as, car des lignes
    -- hors BD TOPO peuvent porter un nom sans bdtopo_cleabs.
    SELECT ranked.*,
           CASE WHEN rn = 1 AND NOT EXISTS (
                    SELECT 1
                    FROM water_entity w
                    WHERE (w.name = ranked.base_name OR w.export_as = ranked.base_name)
                      AND w.bdtopo_cleabs IS DISTINCT FROM ranked.cleabs
                ) THEN ranked.base_name
                ELSE ranked.base_name || '_' || ranked.cleabs
           END AS final_name
    FROM ranked
)
INSERT INTO water_entity (name, export_as, kind, bdtopo_cleabs, geom)
SELECT
    -- name (#134) : toponyme brut, jamais l'escalier (cf. section 1).
    base_name,
    final_name,
    'FLOWING'::water_entity_kind,
    cleabs,
    ST_Force2D(geom)
FROM named
ON CONFLICT (bdtopo_cleabs) DO UPDATE SET
    name = EXCLUDED.name,
    export_as = EXCLUDED.export_as,
    geom = EXCLUDED.geom;

-- ---------------------------------------------------------------------------
-- 3. troncon_hydrographique -> river_section (water_entity_id resolved via
--    liens_vers_cours_d_eau -> water_entity.bdtopo_cleabs)
--
-- liens_vers_cours_d_eau est MULTI-VALUÉ : BD TOPO y liste, séparés par des
-- barres obliques, tous les cours d'eau dont le tronçon fait partie (typiquement
-- un tronçon couvert à la fois par un affluent nommé et par le cours d'eau qui
-- le reçoit). Une égalité stricte sur la chaîne entière ne matche jamais ces
-- lignes : elles perdaient leur rattachement (~6 à 13 % des tronçons liés selon
-- le département).
--
-- RÈGLE DE RATTACHEMENT : parmi les cours d'eau référencés, on retient LE PLUS
-- ÉTENDU (le plus long), départagé par cleabs pour rester déterministe.
--   - L'ordre des éléments dans la chaîne n'est PAS sémantique (il suit le
--     cleabs croissant), d'où un critère explicite plutôt qu'un [1].
--   - Sur les données observées, le cours d'eau le plus court est presque
--     toujours géométriquement inclus dans le plus long : le tronçon est donc
--     rattaché au cours d'eau qui porte tout son cours -- celui que
--     l'utilisateur recherche par son nom -- plutôt qu'à un sous-segment.
--   - La règle inverse (le plus spécifique) laisserait sans aucun tronçon des
--     rivières entières et bien réelles, ce que celle-ci évite.
--
-- Le LATERAL ... LIMIT 1 garantit UNE seule ligne de sortie par tronçon : une
-- jointure sur = ANY(...) dupliquerait les lignes et le ON CONFLICT ci-dessous
-- refuserait de traiter deux fois la même clé.
-- ---------------------------------------------------------------------------

INSERT INTO river_section (water_entity_id, bdtopo_cleabs, persistent, width_class, flow_direction, geom)
SELECT
    parent.id,
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
LEFT JOIN LATERAL (
    SELECT we.id
    FROM unnest(string_to_array(t.liens_vers_cours_d_eau, '/')) AS lien(cleabs)
    JOIN water_entity we ON we.bdtopo_cleabs = lien.cleabs
    ORDER BY ST_Length(we.geom::geography) DESC, we.bdtopo_cleabs
    LIMIT 1
) parent ON true
ON CONFLICT (bdtopo_cleabs) DO UPDATE SET
    water_entity_id = EXCLUDED.water_entity_id,
    persistent = EXCLUDED.persistent,
    width_class = EXCLUDED.width_class,
    flow_direction = EXCLUDED.flow_direction,
    geom = EXCLUDED.geom;

-- ---------------------------------------------------------------------------
-- 4. surface_hydrographique -> water_surface (water_entity_id resolved via
--    liens_vers_plan_d_eau or liens_vers_cours_d_eau -> water_entity.bdtopo_cleabs)
--
-- Mêmes liens multi-valués qu'en section 3, et même règle : on retient l'entité
-- la plus étendue -- aire pour un plan d'eau, longueur pour un cours d'eau.
-- La priorité « plan d'eau avant cours d'eau » de la version précédente est
-- conservée via la colonne priorite.
-- ---------------------------------------------------------------------------

INSERT INTO water_surface (water_entity_id, bdtopo_cleabs, nature, geom)
SELECT
    parent.id,
    s.cleabs,
    s.nature,
    ST_Multi(ST_Force2D(s.geom))
FROM bdtopo_raw.surface_hydrographique s
LEFT JOIN LATERAL (
    SELECT we.id
    FROM (
        SELECT 1 AS priorite, lien AS cleabs
        FROM unnest(string_to_array(s.liens_vers_plan_d_eau, '/')) AS lien
        UNION ALL
        SELECT 2 AS priorite, lien AS cleabs
        FROM unnest(string_to_array(s.liens_vers_cours_d_eau, '/')) AS lien
    ) cand
    JOIN water_entity we ON we.bdtopo_cleabs = cand.cleabs
    ORDER BY cand.priorite,
             CASE WHEN we.kind = 'STILL' THEN ST_Area(we.geom::geography)
                  ELSE ST_Length(we.geom::geography) END DESC,
             we.bdtopo_cleabs
    LIMIT 1
) parent ON true
ON CONFLICT (bdtopo_cleabs) DO UPDATE SET
    water_entity_id = EXCLUDED.water_entity_id,
    nature = EXCLUDED.nature,
    geom = EXCLUDED.geom;
