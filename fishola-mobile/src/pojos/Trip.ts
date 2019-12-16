
import Catch from '@/pojos/Catch';

export default class Trip {

    id?:string;
    name?:string;
    lake?:string;
    date?:string;
    duration?:string;
    catchs:Catch[];

    constructor() {
            this.catchs = [];
    }

}
