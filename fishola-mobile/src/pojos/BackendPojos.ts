/* tslint:disable */
/* eslint-disable */

export interface CatchBean {
    id: string;
    speciesId?: string;
    otherSpecies?: string;
    size?: number;
    weight?: number;
    keep: boolean;
    releasedStateId?: string;
    techniqueId: string;
    description?: string;
    caughtAt?: string;
    sampleId?: string;
    latitude?: number;
    longitude?: number;
    hasPicture: boolean;
    tripId?: string;
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
    startedAt: string;
    finishedAt: string;
    weatherId: string;
    catchs: CatchBean[];
    techniqueIds: string[];
    saveDelayMarker?: Date;
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
    mandatorySize: boolean;
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
    mandatorySize: boolean;
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

export interface SampleType extends Serializable {
    id: string;
    name: string;
    exportAs: string;
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

export interface UpdatePasswordBean {
    currentPassword: string;
    newPassword: string;
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
    latestTripsCatchs: DashboardLastTrip[];
    averageCatchsPerTrip?: number;
    topBySize: { [index: string]: CatchBean[] };
    topByWeight: { [index: string]: CatchBean[] };
}

export interface Serializable {
}

export interface DashboardLastTrip {
    tripId: string;
    day: Date;
    catchsCount: number;
}

export type TripMode = "Live" | "Afterwards";

export type TripType = "Border" | "Craft";
