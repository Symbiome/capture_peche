-- Validation hydrographique de la sortie (#9). La colonne begin_position (point
-- GPS saisi) existe déjà ; on ajoute le « 2e point GPS » de la note Q1 (point
-- projeté sur l'entité hydro attribuée) et la trace de la validation utilisateur.
-- river_section_id existe déjà (jamais écrit à ce jour) : il sera désormais
-- renseigné par le serveur au rattachement.

ALTER TABLE public.trip
    ADD COLUMN snapped_position public.geometry(Point, 4326),
    ADD COLUMN snapped_latitude double precision GENERATED ALWAYS AS (public.st_y(snapped_position)) STORED,
    ADD COLUMN snapped_longitude double precision GENERATED ALWAYS AS (public.st_x(snapped_position)) STORED,
    ADD COLUMN hydro_validation text;

-- Enum applicative (style « text + check » ; les vrais types enum Postgres du
-- schéma restent réservés aux référentiels stables). PENDING est réservé au
-- flux offline (#10) : re-validation à la reconnexion.
ALTER TABLE public.trip
    ADD CONSTRAINT trip_hydro_validation_check
    CHECK (hydro_validation IS NULL OR hydro_validation IN ('CONFIRMED', 'OVERRIDDEN', 'PENDING'));

COMMENT ON COLUMN public.trip.snapped_position IS 'Point projeté sur la géométrie de l''entité hydro attribuée (2e point GPS de la note Q1) ; NULL pour une sortie saisie sans position';
COMMENT ON COLUMN public.trip.snapped_latitude IS 'Latitude du point projeté (colonne générée, comme begin_latitude)';
COMMENT ON COLUMN public.trip.snapped_longitude IS 'Longitude du point projeté (colonne générée, comme begin_longitude)';
COMMENT ON COLUMN public.trip.hydro_validation IS 'Trace de la validation utilisateur de l''attribution hydro : CONFIRMED (entité = plus proche du point), OVERRIDDEN (choix d''une alternative), PENDING (réservé au flux offline #10) ; NULL si sortie sans position';

-- Index sur les colonnes référençant water_entity : le snap d'attribution
-- (HydroSearchDao.SNAP_FOR_ENTITY_SQL) filtre par water_entity_id ; PostgreSQL
-- n'indexe pas automatiquement le côté référençant d'une clé étrangère.
CREATE INDEX IF NOT EXISTS river_section_water_entity_id_idx ON public.river_section (water_entity_id);
CREATE INDEX IF NOT EXISTS water_surface_water_entity_id_idx ON public.water_surface (water_entity_id);
