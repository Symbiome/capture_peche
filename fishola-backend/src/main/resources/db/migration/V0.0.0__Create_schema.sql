CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE gender
    AS ENUM('Male', 'Female', 'NonBinary');

CREATE TABLE fishola_user (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name TEXT,
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    gender gender,
    birth_year INT
);

CREATE TABLE lake (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE weather (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE species (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    built_in BOOLEAN NOT NULL
);

CREATE TABLE species_by_lake (
    lake_id UUID REFERENCES lake(id) NOT NULL,
    species_id UUID REFERENCES species(id) NOT NULL,
    alias TEXT
);

CREATE UNIQUE INDEX species_by_lake_unique_idx
    ON species_by_lake(lake_id, species_id);

CREATE TABLE technique (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    built_in BOOLEAN NOT NULL
);

CREATE TABLE released_fish_state (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TYPE trip_mode
    AS ENUM('Live', 'Afterwards');

CREATE TYPE trip_type
    AS ENUM('Border', 'Craft');

CREATE TABLE trip (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    created_on TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    owner_id UUID REFERENCES fishola_user(id),
    mode trip_mode NOT NULL,
    name TEXT NOT NULL,
    day DATE NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    type trip_type NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    lake_id UUID REFERENCES lake(id) NOT NULL,
    weather_id UUID REFERENCES weather(id) NOT NULL,
    hidden BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE trip_expected_species (
    trip_id UUID REFERENCES trip(id) NOT NULL,
    species_id UUID REFERENCES species(id) NOT NULL
);

CREATE UNIQUE INDEX trip_expected_species_unique_idx
    ON trip_expected_species(trip_id, species_id);

CREATE TABLE trip_techniques (
    trip_id UUID REFERENCES trip(id) NOT NULL,
    technique_id UUID REFERENCES technique(id) NOT NULL
);

CREATE UNIQUE INDEX trip_techniques_unique_idx
    ON trip_techniques(trip_id, technique_id);

CREATE TABLE catch (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    created_on TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    trip_id UUID REFERENCES trip(id) NOT NULL,
    catch_time TIME WITHOUT TIME ZONE,
    species_id UUID REFERENCES species(id) NOT NULL,
    technique_id UUID REFERENCES technique(id) NOT NULL,
    size INT NOT NULL,
    weight INT,
    kept BOOLEAN NOT NULL,
    released_fish_state_id UUID REFERENCES released_fish_state(id),
    description TEXT
);

CREATE TABLE catch_picture (
    catch_id UUID REFERENCES catch(id) PRIMARY KEY,
    content BYTEA NOT NULL
);

CREATE TABLE editorial (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    content TEXT NOT NULL,
    link TEXT
);

CREATE UNIQUE INDEX editorial_unique_idx
    ON editorial(name);

CREATE TABLE documentation (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    content BYTEA
);

CREATE UNIQUE INDEX documentation_unique_idx
    ON documentation(name);
