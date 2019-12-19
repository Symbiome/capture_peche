
import Catch from '@/pojos/Catch';

export default class Trip {

    id?:string;
    type?:string;
    name?:string;
    lakeId?:string;
    speciesIds:string[];
    date?:Date;
    duration?:string;
    catchs:Catch[];

    constructor(public mode:string) {
            this.catchs = [];
            this.speciesIds = [];
    }

}
