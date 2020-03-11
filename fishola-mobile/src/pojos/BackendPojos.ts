/* tslint:disable */
/* eslint-disable */

export interface CatchBean {
    id: string;
    speciesId?: string;
    otherSpecies?: string;
    size: number;
    weight?: number;
    keep: boolean;
    releasedStateId?: string;
    techniqueId: string;
    description?: string;
    caughtAt?: Date;
    withSample: boolean;
    latitude?: number;
    longitude?: number;
    hasPicture: boolean;
}

export interface TripBean {
    id: string;
    createdOn?: Date;
    mode: TripMode;
    type: TripType;
    name: string;
    lakeId: string;
    speciesIds: string[];
    otherSpecies: string;
    date: Date;
    startedAt: Date;
    finishedAt: Date;
    weatherId: string;
    catchs: CatchBean[];
    techniqueIds: string[];
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
    exportAs: string;
    latitude: number;
    longitude: number;
}

export interface ReleasedFishState extends Serializable {
    id: string;
    name: string;
    exportAs: string;
}

export interface Species extends Serializable {
    id: string;
    name: string;
    exportAs: string;
    builtIn: boolean;
}

export interface Technique extends Serializable {
    id: string;
    name: string;
    exportAs: string;
    builtIn: boolean;
}

export interface Weather extends Serializable {
    id: string;
    name: string;
    exportAs: string;
}

export interface Editorial extends Serializable {
    id: string;
    name: string;
    content: string;
    link: string;
}

export interface DocumentationLight {
    id: string;
    name: string;
    url: string;
}

export interface UserSettings {
    promptWeight: boolean;
    promptSamples: boolean;
}

export interface Feedback {
    id: string;
    category: string;
    userId?: string;
    email?: string;
    description?: string;
    screenshot?: string;
    browser?: string;
    os?: string;
    platform?: string;
    screenResolution?: string;
    displaySize?: string;
    locale?: string;
    version?: string;
    date?: Date;
    location?: string;
    locationTitle?: string;
}

export interface Dashboard {
    caughtSpeciesDistribution: { [index: string]: number };
}

export interface Serializable {
}

export type TripMode = "Live" | "Afterwards";

export type TripType = "Border" | "Craft";
