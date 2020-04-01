export default interface CatchSummary {

    id: string;
    speciesId?: string;
    otherSpecies?: string;
    size?: number;
    weight?: number;
    keep?: boolean;
    releasedStateId?: string;
    techniqueId?: string;
    description?: string;
    caughtAt?: string;
    withSample: boolean;
    sampleId?: string;
    hasPicture?: boolean;
    latitude?:number;
    longitude?:number;

}
