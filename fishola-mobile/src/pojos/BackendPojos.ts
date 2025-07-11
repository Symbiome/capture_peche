/* tslint:disable */
/* eslint-disable */

export interface CatchBean {
    id: string;
    speciesId?: string;
    otherSpecies?: string;
    size?: number;
    automaticMeasure?: number;
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
    pictureOrders: number[];
    hasMeasurementPicture: boolean;
    tripId?: string;
    editedSpeciesId?: string;
    editedSize?: number;
    editedWeight?: number;
    excludeFromExport: boolean;
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
    source: DeviceType;
    frontendVersion?: string;
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
    minSize: number;
    maxSize: number;
}

export interface Lake extends Serializable {
    id: string;
    name: string;
    exportAs: string;
    latitude: number;
    longitude: number;
    lakeCode: string;
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

export interface NewsBean {
    id: string;
    name: string;
    content: string;
    datePublicationDebut: Date;
    datePublicationFin: Date;
    dateNotificationSent: Date;
    miniatureId: string;
    isNational: boolean;
    lakeIds: string[];
}

export interface UserSettings {
    promptWeight: boolean;
    promptSamples: boolean;
}

export interface UpdatePasswordBean {
    currentPassword: string;
    newPassword: string;
}

export interface KeyFigures {
    tripsCount: number;
    catchsCount: number;
    picturesCount: number;
    lakes: Lake[];
    catchsCountPerLakeId: { [index: string]: number };
    titleText: string;
    contributeText: string;
    computedOn: Date;
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
    caughtAndReleasedSpeciesDistribution: { [index: string]: number };
    latestTripsCatchs: DashboardLastTrip[];
    averageCatchsPerTrip?: number;
    topBySize: { [index: string]: CatchBean[] };
    topByWeight: { [index: string]: CatchBean[] };
    speciesAliases: { [index: string]: string[] };
    orderedMonths: Month[];
    monthlySizes: { [index: string]: { [P in Month]?: number } };
    monthlySizesPerMaillage: { [index: string]: { [P in Month]?: { [P in Maillage]?: number } } };
    picturesPerTrip: PicturePerTripBean[];
}

export interface GlobalDashboard {
    caughtSpeciesCount: { [index: string]: number };
    caughtSpeciesDistribution: { [index: string]: number };
    caughtAndReleasedSpeciesDistribution: { [index: string]: number };
    speciesAliases: { [index: string]: string[] };
    orderedMonths: Month[];
    monthlySizes: { [index: string]: { [P in Month]?: number } };
    monthlySizesPerMaillage: { [index: string]: { [P in Month]?: { [P in Maillage]?: number } } };
    computedOn: Date;
}

export interface LicenceFromClientBean {
    name: string;
    expirationDate: Date;
    type: LicenceType;
    content: string;
}

export interface LicenceResponseBean {
    id: string;
    name: string;
    userId: string;
    expirationDate: Date;
    type: LicenceType;
}

export interface CatchMarker {
    id: string;
    date: Date;
    tripId: string;
    tripName: string;
    specieName: string;
    lakeName: string;
    latitude: number;
    longitude: number;
    size: number;
    weight: number;
    maillage: Maillage;
    hasValidCoordinates: boolean;
}

export interface TripSocial {
    id: string;
    tripName: string;
    userName: string;
    lakeName: string;
    date: Date;
    durationInSeconds: number;
    socialReactions: TripSocialReaction[];
    catchesCountPerMaillage: { [index: string]: { [P in Maillage]?: number } };
}

export interface Serializable {
}

export interface DashboardLastTrip {
    tripId: string;
    day: Date;
    catchsCount: number;
}

export interface PicturePerTripBean {
    tripDate: Date;
    tripId: string;
    tripName: string;
    tripLakeName: string;
    pictureURLs: string[];
}

export interface TripSocialReaction extends Serializable {
    tripId: string;
    userId: string;
    message: string;
    reaction: SocialReaction;
}

export type TripMode = "Live" | "Afterwards";

export type TripType = "Border" | "Craft";

export type DeviceType = "web" | "application";

export type Month = "JANUARY" | "FEBRUARY" | "MARCH" | "APRIL" | "MAY" | "JUNE" | "JULY" | "AUGUST" | "SEPTEMBER" | "OCTOBER" | "NOVEMBER" | "DECEMBER";

export type Maillage = "MAILLEE" | "NON_MAILLEE" | "NON_DEFINI";

export type LicenceType = "PDF" | "JPEG" | "PNG";

export type SocialReaction = "LIKE" | "LOVE" | "LETS_MEET";
