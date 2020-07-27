/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
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
    weatherId?: string;
    catchs: CatchBean[];
    techniqueIds: string[];
    beginLatitude?: number;
    beginLongitude?: number;
    endLatitude?: number;
    endLongitude?: number;
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
    authorizedSample: boolean;
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
    naturalId: string;
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
    devicePixelRatio?: string;
    displaySize?: string;
    locale?: string;
    frontendVersion?: string;
    backendVersion?: string;
    date?: Date;
    location?: string;
    locationTitle?: string;
    device?: string;
}

export interface Dashboard {
    caughtSpeciesCount: { [index: string]: number };
    caughtSpeciesDistribution: { [index: string]: number };
    latestTripsCatchs: DashboardLastTrip[];
    averageCatchsPerTrip?: number;
    topBySize: { [index: string]: CatchBean[] };
    topByWeight: { [index: string]: CatchBean[] };
    speciesAliases: { [index: string]: string[] };
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
