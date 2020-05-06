
CREATE TABLE authorized_sample (
    lake_id UUID REFERENCES lake(id) NOT NULL,
    species_id UUID REFERENCES species(id) NOT NULL,
    PRIMARY KEY (lake_id, species_id)
);

update flyway_schema_history set checksum = 461691641 where version='0.0.0';
