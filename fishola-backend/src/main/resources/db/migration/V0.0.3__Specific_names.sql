INSERT INTO species_by_lake (lake_id, species_id)
    SELECT l.id AS lake_id, s.id AS species_id
    FROM lake l, species s;

UPDATE species_by_lake
    SET ALIAS = 'Lavaret'
    WHERE lake_id = ( SELECT id FROM lake WHERE name = 'Lac du Bourget' )
    AND species_id = ( SELECT id FROM species WHERE name = 'Corégone' );

UPDATE species_by_lake
    SET ALIAS = 'Lavaret'
    WHERE lake_id = ( SELECT id FROM lake WHERE name = 'Lac d''Aiguebelette' )
    AND species_id = ( SELECT id FROM species WHERE name = 'Corégone' );

UPDATE species_by_lake
    SET ALIAS = 'Féra'
    WHERE lake_id = ( SELECT id FROM lake WHERE name = 'Léman' )
    AND species_id = ( SELECT id FROM species WHERE name = 'Corégone' );

UPDATE species_by_lake
    SET ALIAS = 'Féra'
    WHERE lake_id = ( SELECT id FROM lake WHERE name = 'Lac d''Annecy' )
    AND species_id = ( SELECT id FROM species WHERE name = 'Corégone' );
