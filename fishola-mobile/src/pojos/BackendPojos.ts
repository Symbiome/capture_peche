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

export interface CatchBean {
}

export type TripMode = "Live" | "Afterwards";

export type TripType = "Border" | "Craft";
