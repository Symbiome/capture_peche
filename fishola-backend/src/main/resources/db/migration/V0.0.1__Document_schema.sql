COMMENT ON TYPE gender IS 'Genres de l''utilisateur';

COMMENT ON TABLE fishola_user IS 'Comptes utilisateur';
COMMENT ON COLUMN fishola_user.id IS 'Identifiant technique';
COMMENT ON COLUMN fishola_user.first_name IS 'Prénom';
COMMENT ON COLUMN fishola_user.last_name IS 'Nom';
COMMENT ON COLUMN fishola_user.email IS 'Email. Il s''agit également de l''identifiant de connexion. C''est donc une clé naturelle';
COMMENT ON COLUMN fishola_user.password IS 'Mot de passe';
COMMENT ON COLUMN fishola_user.gender IS 'Genre';
COMMENT ON COLUMN fishola_user.birth_year IS 'Année de naissance';

COMMENT ON TABLE lake IS 'Lacs';
COMMENT ON COLUMN lake.id IS 'Identifiant technique';
COMMENT ON COLUMN lake.name IS 'Nom du lac';

COMMENT ON TABLE species IS 'Espèces';
COMMENT ON COLUMN species.id IS 'Identifiant technique';
COMMENT ON COLUMN species.name IS 'Nom de l''espèce';
COMMENT ON COLUMN species.built_in IS 'Est-ce que la méthode a été créée par les admins ?';

COMMENT ON TABLE species_by_lake IS 'Espèces par lac';
COMMENT ON COLUMN species_by_lake.lake_id IS 'Lac concerné';
COMMENT ON COLUMN species_by_lake.species_id IS 'Espèce concernée';
COMMENT ON COLUMN species_by_lake.alias IS 'Alias éventuel sur l''espèce a une appelation différente sur ce lac';

COMMENT ON TABLE weather IS 'Météos';
COMMENT ON COLUMN weather.id IS 'Identifiant technique';
COMMENT ON COLUMN weather.name IS 'Nom de la météo';

COMMENT ON TABLE technique IS 'Techniques de pêche';
COMMENT ON COLUMN technique.id IS 'Identifiant technique';
COMMENT ON COLUMN technique.name IS 'Nom de la technique';
COMMENT ON COLUMN technique.built_in IS 'Est-ce que la technique a été créée par les admins ?';

COMMENT ON TABLE released_fish_state IS 'État dans lequel le poisson a été relâché';
COMMENT ON COLUMN released_fish_state.id IS 'Identifiant technique';
COMMENT ON COLUMN released_fish_state.name IS 'Nom';

COMMENT ON TYPE trip_type IS 'Types de sortie';

COMMENT ON TABLE trip IS 'Sorties de pêche';
COMMENT ON COLUMN trip.id IS 'Identifiant technique';
COMMENT ON COLUMN trip.owner_id IS 'Utilisateur ayant fait la pêche. Peut être nul si l''utilisateur a utilisé son droit à l''oubli RGPD';
COMMENT ON COLUMN trip.mode IS 'Sortie en mode ''Live'' ou ''Afterwards''';
COMMENT ON COLUMN trip.name IS 'Nom donné par l''utilisateur ou généré automatiquement';
COMMENT ON COLUMN trip.day IS 'Jour';
COMMENT ON COLUMN trip.start_time IS 'Heure de début';
COMMENT ON COLUMN trip.end_time IS 'Heure de fin';
COMMENT ON COLUMN trip.type IS 'Type de sortie';
COMMENT ON COLUMN trip.latitude IS 'Latitude';
COMMENT ON COLUMN trip.longitude IS 'Longitude';
COMMENT ON COLUMN trip.lake_id IS 'Lac sur lequel se fait la sortie';
COMMENT ON COLUMN trip.weather_id IS 'Météo de la sortie';

COMMENT ON TABLE trip_expected_species IS 'Espèces recherchées pendant une sortie';
COMMENT ON COLUMN trip_expected_species.trip_id IS 'Identifiant de la sortie';
COMMENT ON COLUMN trip_expected_species.species_id IS 'Identifiant de l''espèce';

COMMENT ON TABLE trip_techniques IS 'Techniques de pêche utilisées pendant une sortie';
COMMENT ON COLUMN trip_techniques.trip_id IS 'Identifiant de la sortie';
COMMENT ON COLUMN trip_techniques.technique_id IS 'Identifiant de la technique de pêche';

COMMENT ON TABLE catch IS 'Captures';
COMMENT ON COLUMN catch.id IS 'Identifiant technique';
COMMENT ON COLUMN catch.trip_id IS 'Sortie concernée';
COMMENT ON COLUMN catch.catch_time IS 'Heure de la capture';
COMMENT ON COLUMN catch.species_id IS 'Espèce capturée';
COMMENT ON COLUMN catch.technique_id IS 'Technique de pêche de la capture';
COMMENT ON COLUMN catch.picture IS 'Photo';
COMMENT ON COLUMN catch.size IS 'Taille du poisson (cm)';
COMMENT ON COLUMN catch.weight IS 'Poids du poisson (g)';
COMMENT ON COLUMN catch.kept IS 'Est-ce que le poisson a été conservé ?';
COMMENT ON COLUMN catch.released_fish_state_id IS 'Si il a été relâché, dans quel état ?';
COMMENT ON COLUMN catch.description IS 'Description';
