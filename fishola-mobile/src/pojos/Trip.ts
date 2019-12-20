
import Catch from '@/pojos/Catch';

export default class Trip {

    id?:string;
    mode?:string;
    type?:string;
    name?:string;
    lakeId?:string;
    speciesIds:string[];
    date?:Date;
    startedAt?:Date;
    finishedAt?:Date;
    duration?:string;
    catchs:Catch[];

    constructor() {
            this.catchs = [];
            this.speciesIds = [];
    }

}
