
export default interface TripMain {

    id: string;
    name: string;
    mode: string;
    startedAt: string;
    finishedAt?: string;
    catchs: any[];
    modifiableUntil?: Date;

}
