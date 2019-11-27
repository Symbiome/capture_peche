INSERT INTO species_by_lake (lake, species)
    SELECT l.id AS lake_id, s.id AS species_id
    FROM lake l, species s;

UPDATE species_by_lake
    SET ALIAS = 'Lavaret'
    WHERE lake = ( SELECT id FROM lake WHERE name = 'Lac du Bourget' )
    AND species = ( SELECT id FROM species WHERE name = 'Corégone' );

UPDATE species_by_lake
    SET ALIAS = 'Lavaret'
    WHERE lake = ( SELECT id FROM lake WHERE name = 'Lac d''Aiguebelette' )
    AND species = ( SELECT id FROM species WHERE name = 'Corégone' );

UPDATE species_by_lake
    SET ALIAS = 'Féra'
    WHERE lake = ( SELECT id FROM lake WHERE name = 'Lac Léman' )
    AND species = ( SELECT id FROM species WHERE name = 'Corégone' );

UPDATE species_by_lake
    SET ALIAS = 'Féra'
    WHERE lake = ( SELECT id FROM lake WHERE name = 'Lac d''Annecy' )
    AND species = ( SELECT id FROM species WHERE name = 'Corégone' );
