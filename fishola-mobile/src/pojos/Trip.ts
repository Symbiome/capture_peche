
import Catch from '@/pojos/Catch';

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
            let seconds = Math.floor((end.getTime()-startedAt.getTime())/1000);
            let minutes = Math.floor(seconds/60);
            let hours = Math.floor(minutes/60);
            let result = '';
            if (hours > 0) {
                result += hours + 'h ';
                minutes -= hours * 60;
            }
            if (minutes > 0) {
                result += minutes + 'min ';
                seconds -= hours * 60*60 + minutes * 60;
            }
            result += seconds + 's';
            return result;
        }
        return '';
    }


}
