CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE gender
  AS ENUM('Male', 'Female', 'NonBinary');

create table fishola_user (
    id uuid DEFAULT uuid_generate_v4 () not null primary key,
    first_name text NOT NULL,
    last_name text,
    email text NOT NULL UNIQUE,
    password text NOT NULL,
    gender gender,
    birth_year int
);

create table lake (
    id uuid DEFAULT uuid_generate_v4 () not null primary key,
    name text NOT NULL UNIQUE
);

create table weather (
    id uuid DEFAULT uuid_generate_v4 () not null primary key,
    name text NOT NULL UNIQUE
);

create table species (
    id uuid DEFAULT uuid_generate_v4 () not null primary key,
    name text NOT NULL UNIQUE,
    buildt_in boolean NOT NULL
);

create table method (
    id uuid DEFAULT uuid_generate_v4 () not null primary key,
    name text NOT NULL UNIQUE,
    buildt_in boolean NOT NULL
);

create table released_fish_state (
    id uuid DEFAULT uuid_generate_v4 () not null primary key,
    name text NOT NULL UNIQUE
);

CREATE TYPE trip_type
  AS ENUM('Border', 'Craft');

create table trip (
    id uuid DEFAULT uuid_generate_v4 () not null primary key,
    owner uuid REFERENCES fishola_user(id),
    live boolean NOT NULL,
    name text NOT NULL,
    day date NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone NOT NULL,
    type trip_type NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    lake uuid REFERENCES lake(id) NOT NULL,
    weather uuid REFERENCES weather(id) NOT NULL
);

create table trip_expected_species (
    trip uuid REFERENCES trip(id) NOT NULL,
    species uuid REFERENCES species(id) NOT NULL
);

CREATE UNIQUE INDEX trip_expected_species_idx on trip_expected_species(trip, species);

create table trip_methods (
    trip uuid REFERENCES trip(id) NOT NULL,
    method uuid REFERENCES method(id) NOT NULL
);

CREATE UNIQUE INDEX trip_methods_idx on trip_methods(trip, method);

create table catch (
    id uuid DEFAULT uuid_generate_v4 () not null primary key,
    trip uuid REFERENCES trip(id) NOT NULL,
    catch_time timestamp without time zone NOT NULL,
    species uuid REFERENCES species(id) NOT NULL,
    method uuid REFERENCES method(id) NOT NULL,
    picture oid,
    size double precision,
    weight double precision,
    kept boolean NOT NULL,
    released_fish_state uuid REFERENCES released_fish_state(id),
    description text
);
