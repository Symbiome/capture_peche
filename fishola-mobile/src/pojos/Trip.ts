
import Catch from '@/pojos/Catch';
import Helpers from '@/pojos/Helpers';

export default class Trip {

    id?:string;
    dirty:boolean;
    mode?:string;
    type?:string;
    name?:string;
    lakeId?:string;
    speciesIds:string[];
    date?:Date;
    startedAt?:Date;
    finishedAt?:Date;
    catchs:Catch[];

    constructor() {
        this.dirty = false;
        this.catchs = [];
        this.speciesIds = [];
    }

    static computeDuration(input:Trip):string {
        let startedAt = input.startedAt;
        if (startedAt) {
            let end = input.finishedAt || new Date();
            let result = Helpers.computeDuration(startedAt, end);
            return result;
        }
        return '';
    }

}
