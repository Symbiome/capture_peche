import {TripType} from '@/pojos/BackendPojos';

export default interface TripSummary {

    id: string;
    name: string;
    mode: string;
    lakeId: string;
    date: Date;
    type: TripType;
    startedAt: string;
    finishedAt?: string;
    modifiableUntil?: Date;
    speciesIds: string[];
    otherSpecies: string;
    weatherId?: string;
    techniqueIds: string[];

}
