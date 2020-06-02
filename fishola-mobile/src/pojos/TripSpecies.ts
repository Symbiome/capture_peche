
export default interface TripSpecies {

    id: string;
    lakeId: string;
    mode: string;
    startedAt: string;
    speciesIds: string[];
    otherSpecies: string;
    beginLatitude?: number;
    beginLongitude?: number;

}
