alter table trip alter column weather_id drop not null ;
update flyway_schema_history set checksum = 1939660525 where version='0.0.0';
