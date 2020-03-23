
import {TripType, TripMode} from '@/pojos/BackendPojos';

export default interface TripMeta {

    id: string;
    name?: string;
    lakeId?: string;
    mode: TripMode;
    type?: TripType;
    date?:Date;
    startedAt?: string;
    finishedAt?: string;

}
