
export default interface TripMain {

    id: string;
    name: string;
    mode: string;
    startedAt: Date;
    finishedAt?: Date;
    catchs: any[];
    modifiableUntil?: Date;

}
