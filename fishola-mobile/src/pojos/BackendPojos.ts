/* tslint:disable */
/* eslint-disable */

export interface TripBean {
    id: string;
    createdOn?: Date;
    mode: TripMode;
    type: TripType;
    name: string;
    lakeId: string;
    speciesIds: string[];
    date: Date;
    startedAt: Date;
    finishedAt: Date;
    weatherId: string;
    catchs: CatchBean[];
    modifiableUntil?: Date;
}

export interface TripLight {
    id: string;
    name: string;
    lakeId: string;
    date: Date;
    durationInSeconds: number;
    catchsCount: number;
    modifiable: boolean;
}

export interface SpeciesWithAlias {
    id: string;
    name: string;
    builtIn: boolean;
    alias?: string;
}

export interface Lake extends Serializable {
    id: string;
    name: string;
}

export interface ReleasedFishState extends Serializable {
    id: string;
    name: string;
}

export interface Species extends Serializable {
    id: string;
    name: string;
    builtIn: boolean;
}

export interface Technique extends Serializable {
    id: string;
    name: string;
    builtIn: boolean;
}

export interface Weather extends Serializable {
    id: string;
    name: string;
}

export interface CatchBean {
}

export interface Serializable {
}

export type TripMode = "Live" | "Afterwards";

export type TripType = "Border" | "Craft";
