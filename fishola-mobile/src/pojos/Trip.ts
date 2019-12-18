
import Catch from '@/pojos/Catch';

export default class Trip {

    id?:string;
    // type:string;
    name?:string;
    lake?:string;
    date?:string;
    duration?:string;
    catchs:Catch[];

    constructor(public type:string) {
            this.catchs = [];
    }

}
