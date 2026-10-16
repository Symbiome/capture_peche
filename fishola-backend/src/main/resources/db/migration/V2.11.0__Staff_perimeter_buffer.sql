-- Périmètre staff élargi (#231). Jusqu'ici un admin local / opérateur ne gérait
-- que les milieux dont water_entity.department (UN seul département, celui de
-- plus grand recouvrement, #154) figurait dans son périmètre : un étang à cheval
-- sur une limite, ou un cours d'eau frontière (Rhône, Saône, Ain…), échappait à
-- la fédération voisine.
--
-- Désormais un milieu relève de tout département dont le contour élargi d'un
-- buffer (1 km par défaut, `fishola.staff-perimeter-buffer-m`) intersecte sa
-- géométrie. Correspondance précalculée dans water_entity_department ;
-- water_entity.department reste le département « principal » (affichage,
-- exports, trip.department / catch.department) et fait toujours partie du
-- périmètre (repli quand le contour départemental n'est pas chargé).
--
-- Seule la GESTION des milieux est concernée : les données (sorties, prises,
-- exports) restent cloisonnées par trip.department / catch.department.

-- Distance du buffer en mètres. Une seule ligne ; synchronisée au démarrage
-- avec la configuration (StaffPerimeterService), 1000 m par défaut.
CREATE TABLE staff_perimeter_setting (
    id boolean DEFAULT true NOT NULL,
    buffer_m double precision NOT NULL,
    CONSTRAINT staff_perimeter_setting_pkey PRIMARY KEY (id),
    CONSTRAINT staff_perimeter_setting_single_row CHECK (id),
    CONSTRAINT staff_perimeter_setting_buffer_positive CHECK (buffer_m >= 0)
);

COMMENT ON TABLE staff_perimeter_setting IS 'Distance du buffer autour des départements pour le périmètre staff (#231), ligne unique';
COMMENT ON COLUMN staff_perimeter_setting.buffer_m IS 'Distance du buffer en mètres (configuration fishola.staff-perimeter-buffer-m)';

INSERT INTO staff_perimeter_setting (id, buffer_m) VALUES (true, 1000);

-- Contours départementaux élargis du buffer, découpés (ST_Subdivide) pour que
-- les ST_Intersects par entité restent indexés et rapides. Le contour BD TOPO est
-- simplifié à ~10 m puis découpé AVANT le buffer (buffer(A ∪ B) = buffer(A) ∪
-- buffer(B)) : sur les 101 départements, ~11 s au lieu de ~70 s pour un buffer
-- du contour complet, pour 10 correspondances d'écart sur ~204 000.
CREATE TABLE departement_buffer (
    code character varying(3) NOT NULL,
    geom geometry(Geometry, 4326) NOT NULL
);

CREATE INDEX departement_buffer_geom_idx ON departement_buffer USING gist (geom);

COMMENT ON TABLE departement_buffer IS 'Contours départementaux élargis du buffer staff (#231), découpés pour l''indexation';
COMMENT ON COLUMN departement_buffer.code IS 'Code INSEE du département';
COMMENT ON COLUMN departement_buffer.geom IS 'Morceau du contour élargi (EPSG:4326)';

CREATE TABLE water_entity_department (
    water_entity_id uuid NOT NULL,
    department_code character varying(3) NOT NULL,
    CONSTRAINT water_entity_department_pkey PRIMARY KEY (water_entity_id, department_code),
    CONSTRAINT water_entity_department_water_entity_fkey FOREIGN KEY (water_entity_id)
        REFERENCES water_entity (id) ON DELETE CASCADE
);

CREATE INDEX water_entity_department_code_idx ON water_entity_department (department_code);

COMMENT ON TABLE water_entity_department IS 'Départements dont le périmètre staff (contour élargi du buffer) couvre le milieu (#231)';
COMMENT ON COLUMN water_entity_department.water_entity_id IS 'Milieu (cours d''eau / plan d''eau)';
COMMENT ON COLUMN water_entity_department.department_code IS 'Code INSEE d''un département dont le staff peut gérer le milieu';

-- Départements du périmètre d'UN milieu : contours élargis intersectés, plus
-- son département principal.
CREATE FUNCTION refresh_water_entity_department(p_water_entity_id uuid) RETURNS void
LANGUAGE sql AS $$
    DELETE FROM water_entity_department WHERE water_entity_id = p_water_entity_id;
    INSERT INTO water_entity_department (water_entity_id, department_code)
    SELECT we.id, b.code
    FROM water_entity we
    JOIN departement_buffer b ON ST_Intersects(b.geom, we.geom)
    WHERE we.id = p_water_entity_id
    UNION
    SELECT we.id, we.department
    FROM water_entity we
    WHERE we.id = p_water_entity_id AND we.department IS NOT NULL;
$$;

-- Recalcul complet : contours élargis puis correspondance de tous les milieux.
-- À rappeler après un chargement des départements (import_departements_parquet)
-- ou un changement de buffer (StaffPerimeterService au démarrage).
CREATE FUNCTION refresh_staff_perimeter() RETURNS void
LANGUAGE sql AS $$
    TRUNCATE departement_buffer;
    INSERT INTO departement_buffer (code, geom)
    SELECT p.code, ST_Buffer(p.geom::geography, s.buffer_m)::geometry
    FROM (
        SELECT d.code, ST_Subdivide(ST_SimplifyPreserveTopology(d.geom, 0.0001), 256) AS geom
        FROM departement d
    ) p
    CROSS JOIN staff_perimeter_setting s;
    TRUNCATE water_entity_department;
    INSERT INTO water_entity_department (water_entity_id, department_code)
    SELECT DISTINCT we.id, b.code
    FROM water_entity we
    JOIN departement_buffer b ON ST_Intersects(b.geom, we.geom)
    UNION
    SELECT we.id, we.department
    FROM water_entity we
    WHERE we.department IS NOT NULL;
$$;

-- Maintenance au fil de l'eau : import hydro (upsert), recalcul de
-- water_entity.department, création d'entité.
CREATE FUNCTION water_entity_department_trigger() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM refresh_water_entity_department(NEW.id);
    RETURN NULL;
END;
$$;

CREATE TRIGGER water_entity_department_refresh
    AFTER INSERT OR UPDATE OF geom, department ON water_entity
    FOR EACH ROW EXECUTE FUNCTION water_entity_department_trigger();

SELECT refresh_staff_perimeter();
