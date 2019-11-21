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

CREATE TABLE method (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    built_in BOOLEAN NOT NULL
);

CREATE TABLE released_fish_state (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TYPE trip_type
    AS ENUM('Border', 'Craft');

CREATE TABLE trip (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    owner UUID REFERENCES fishola_user(id),
    live BOOLEAN NOT NULL,
    name TEXT NOT NULL,
    day DATE NOT NULL,
    start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    type trip_type NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    lake UUID REFERENCES lake(id) NOT NULL,
    weather UUID REFERENCES weather(id) NOT NULL
);

CREATE TABLE trip_expected_species (
    trip UUID REFERENCES trip(id) NOT NULL,
    species UUID REFERENCES species(id) NOT NULL
);

CREATE UNIQUE INDEX trip_expected_species_idx
    ON trip_expected_species(trip, species);

CREATE TABLE trip_methods (
    trip UUID REFERENCES trip(id) NOT NULL,
    method UUID REFERENCES method(id) NOT NULL
);

CREATE UNIQUE INDEX trip_methods_idx
    ON trip_methods(trip, method);

CREATE TABLE catch (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    trip UUID REFERENCES trip(id) NOT NULL,
    catch_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    species UUID REFERENCES species(id) NOT NULL,
    method UUID REFERENCES method(id) NOT NULL,
    picture OID,
    size DOUBLE PRECISION,
    weight DOUBLE PRECISION,
    kept BOOLEAN NOT NULL,
    released_fish_state UUID REFERENCES released_fish_state(id),
    description TEXT
);
