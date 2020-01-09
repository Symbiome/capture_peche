/* tslint:disable */
/* eslint-disable */

export class TripBean {
    id?: string;
    dirty?: boolean;
    mode?: TripMode;
    type?: TripType;
    name?: string;
    lakeId?: string;
    speciesIds?: string[];
    date?: Date;
    startedAt?: Date;
    finishedAt?: Date;
    catchs?: CatchBean[];
    weatherId?: string;
}

export class CatchBean {
}

export type TripMode = "Live" | "Afterwards";

export type TripType = "Border" | "Craft";
