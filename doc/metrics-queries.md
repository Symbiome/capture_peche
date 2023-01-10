## Nombre d'utilisateurs

select count(*) from fishola_user where exclude_from_exports = false and created_on < '2021-01-01';

## Nombre de sorties par lac et par année

select lake.name as lac, extract(year from t.created_on) as annee, count(*) as nombre_de_sorties from trip t join lake on lake.id = t.lake_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false group by lake.name, extract(year from t.created_on) order by lake.name;

## Nombre de prises par lac et par années

select lake.name as lac, extract(year from c.created_on) as annee, count(*) as nombre_de_prises from catch c join trip t on c.trip_id = t.id join lake on lake.id = t.lake_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false group by lake.name, extract(year from c.created_on) order by lake.name;

### Nombre de prises avec mesures auto

select lake.name as lac, extract(year from c.created_on) as annee, count(*) as nombre_de_prises_avec_mesure_auto from catch c join trip t on c.trip_id = t.id join lake on lake.id = t.lake_id join fishola_user u on u.id = t.owner_id where u.exclude_from_exports = false and automatic_measure > 0 group by lake.name, extract(year from c.created_on) order by lake.name;

