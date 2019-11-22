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

COMMENT ON TABLE weather IS 'Météos';
COMMENT ON COLUMN weather.id IS 'Identifiant technique';
COMMENT ON COLUMN weather.name IS 'Nom de la météo';

COMMENT ON TABLE species IS 'Espèces';
COMMENT ON COLUMN species.id IS 'Identifiant technique';
COMMENT ON COLUMN species.name IS 'Nom de l''espèce''';
COMMENT ON COLUMN species.built_in IS 'Est-ce que la méthode a été créée par les admins ?';

COMMENT ON TABLE method IS 'Méthodes de pêche';
COMMENT ON COLUMN method.id IS 'Identifiant technique';
COMMENT ON COLUMN method.name IS 'Nom de la méthode';
COMMENT ON COLUMN method.built_in IS 'Est-ce que la méthode a été créée par les admins ?';

COMMENT ON TABLE released_fish_state IS 'État dans lequel le poisson a été relâché';
COMMENT ON COLUMN released_fish_state.id IS 'Identifiant technique';
COMMENT ON COLUMN released_fish_state.name IS 'Nom';

COMMENT ON TYPE trip_type IS 'Types de sortie';

COMMENT ON TABLE trip IS 'Sorties de pêche';
COMMENT ON COLUMN trip.id IS 'Identifiant technique';
COMMENT ON COLUMN trip.owner IS 'Utilisateur ayant fait la pêche. Peut être nul si l''utilisateur a utilisé son droit à l''oubli RGPD';
COMMENT ON COLUMN trip.live IS 'Est-ce une sortie en live ?';
COMMENT ON COLUMN trip.name IS 'Nom donné par l''utilisateur ou généré automatiquement';
COMMENT ON COLUMN trip.day IS 'Jour';
COMMENT ON COLUMN trip.start_time IS 'Heure de début';
COMMENT ON COLUMN trip.end_time IS 'Heure de fin';
COMMENT ON COLUMN trip.type IS 'Type de sortie';
COMMENT ON COLUMN trip.latitude IS 'Latitude';
COMMENT ON COLUMN trip.longitude IS 'Longitude';
COMMENT ON COLUMN trip.lake IS 'Lac sur lequel se fait la sortie';
COMMENT ON COLUMN trip.weather IS 'Météo de la sortie';

COMMENT ON TABLE trip_expected_species IS 'Espèces recherchées pendant une sortie';
COMMENT ON COLUMN trip_expected_species.trip IS 'Identifiant de la sortie';
COMMENT ON COLUMN trip_expected_species.species IS 'Identifiant de l''espèce';

COMMENT ON TABLE trip_methods IS 'Méthodes de pêche utilisées pendant une sortie';
COMMENT ON COLUMN trip_methods.trip IS 'Identifiant de la sortie';
COMMENT ON COLUMN trip_methods.method IS 'Identifiant de la méthode de pêche';

COMMENT ON TABLE catch IS 'Captures';
COMMENT ON COLUMN catch.id IS 'Identifiant technique';
COMMENT ON COLUMN catch.trip IS 'Sortie concernée';
COMMENT ON COLUMN catch.catch_time IS 'Heure de la capture';
COMMENT ON COLUMN catch.species IS 'Espèce capturée';
COMMENT ON COLUMN catch.method IS 'Méthode de pêche de la capture';
COMMENT ON COLUMN catch.picture IS 'Photo';
COMMENT ON COLUMN catch.size IS 'Taille du poisson (cm)';
COMMENT ON COLUMN catch.weight IS 'Poids du poisson (g)';
COMMENT ON COLUMN catch.kept IS 'Est-ce que le poisson a été conservé ?';
COMMENT ON COLUMN catch.released_fish_state IS 'Si il a été relâché, dans quel état ?';
COMMENT ON COLUMN catch.description IS 'Description';
