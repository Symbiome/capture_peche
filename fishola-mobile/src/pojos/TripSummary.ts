import {TripType} from '@/pojos/BackendPojos';

export default interface TripSummary {

    id: string;
    name: string;
    mode: string;
    lakeId: string;
    date: Date;
    type: TripType;
    startedAt: Date;
    finishedAt?: Date;
    modifiableUntil?: Date;
    speciesIds: string[];
    weatherId?: string;

}
