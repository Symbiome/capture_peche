
import Catch from '@/pojos/Catch';

export default class Trip {

    duration?:string;
    catchs:Catch[] = [];

    constructor(
        public id:string,
        public name:string,
        public lake:string,
        public date:string) {
            this.duration = '4h49min';
    }

}
