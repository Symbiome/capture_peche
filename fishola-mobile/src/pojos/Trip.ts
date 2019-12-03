
import Catch from '@/pojos/Catch';

export default class Trip {

    duration?:string;
    catchs:Catch[];
    canBeModified:boolean;

    constructor(
        public id:string,
        public name:string,
        public lake:string,
        public date:string) {
            this.canBeModified = false;
            this.catchs = [];
    }

}
