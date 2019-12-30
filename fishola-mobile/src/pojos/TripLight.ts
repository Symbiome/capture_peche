export default class TripLight {

    duration?:string;
    catchsCount:number;
    canBeModified:boolean;
    lakeId?:string;

    startedAt?:Date;
    finishedAt?:Date;

    constructor(
        public id:string,
        public name:string,
        public lakeName:string,
        public date:string) {
            this.canBeModified = false;
            this.catchsCount = 0;
    }

}
