CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE gender
    AS ENUM('Male', 'Female', 'NonBinary');

CREATE SEQUENCE sample_base_id_sequence;

CREATE TABLE fishola_user (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name TEXT,
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    gender gender,
    birth_year INT,
    prompt_weight BOOLEAN NOT NULL DEFAULT TRUE,
    prompt_samples BOOLEAN NOT NULL DEFAULT FALSE,
    sample_base_id INT NOT NULL UNIQUE DEFAULT nextval('sample_base_id_sequence')
);

CREATE TABLE lake (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    export_as TEXT NOT NULL UNIQUE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL
);

CREATE TABLE weather (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    export_as TEXT NOT NULL UNIQUE
);

CREATE TABLE species (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    export_as TEXT NOT NULL UNIQUE,
    built_in BOOLEAN NOT NULL,
    mandatory_size BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE species_by_lake (
    lake_id UUID REFERENCES lake(id) NOT NULL,
    species_id UUID REFERENCES species(id) NOT NULL,
    alias TEXT,
    PRIMARY KEY (lake_id, species_id)
);

CREATE TABLE technique (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    export_as TEXT NOT NULL UNIQUE,
    built_in BOOLEAN NOT NULL
);

CREATE TABLE released_fish_state (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    export_as TEXT NOT NULL UNIQUE
);

CREATE TABLE sample_type (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    export_as TEXT NOT NULL UNIQUE
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
    lake_id UUID REFERENCES lake(id) NOT NULL,
    weather_id UUID REFERENCES weather(id) NOT NULL,
    hidden BOOLEAN NOT NULL DEFAULT false,
    begin_latitude DOUBLE PRECISION,
    begin_longitude DOUBLE PRECISION,
    end_latitude DOUBLE PRECISION,
    end_longitude DOUBLE PRECISION
);

CREATE TABLE trip_expected_species (
    trip_id UUID REFERENCES trip(id) NOT NULL,
    species_id UUID REFERENCES species(id) NOT NULL,
    PRIMARY KEY (trip_id, species_id)
);

CREATE TABLE trip_techniques (
    trip_id UUID REFERENCES trip(id) NOT NULL,
    technique_id UUID REFERENCES technique(id) NOT NULL,
    PRIMARY KEY (trip_id, technique_id)
);

CREATE TABLE catch (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    created_on TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    trip_id UUID REFERENCES trip(id) NOT NULL,
    catch_time TIME WITHOUT TIME ZONE,
    species_id UUID REFERENCES species(id) NOT NULL,
    technique_id UUID REFERENCES technique(id) NOT NULL,
    size INT,
    weight INT,
    kept BOOLEAN NOT NULL,
    released_fish_state_id UUID REFERENCES released_fish_state(id),
    description TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    sample_id TEXT
);

CREATE TABLE catch_sample_types (
    catch_id UUID REFERENCES catch(id) NOT NULL,
    sample_type_id UUID REFERENCES sample_type(id) NOT NULL,
    PRIMARY KEY (catch_id, sample_type_id)
);

CREATE TABLE catch_picture (
    catch_id UUID REFERENCES catch(id) PRIMARY KEY,
    content BYTEA NOT NULL
);

CREATE TABLE editorial (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    content TEXT NOT NULL,
    link TEXT
);

CREATE TABLE documentation (
    id UUID DEFAULT uuid_generate_v4() NOT NULL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    content BYTEA
);

CREATE TABLE authorized_sample (
    lake_id UUID REFERENCES lake(id) NOT NULL,
    species_id UUID REFERENCES species(id) NOT NULL,
    PRIMARY KEY (lake_id, species_id)
);
