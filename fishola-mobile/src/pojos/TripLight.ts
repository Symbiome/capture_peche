export default class Trip {

    duration?:string;
    catchs:number;
    canBeModified:boolean;

    constructor(
        public id:string,
        public name:string,
        public lakeName:string,
        public date:string) {
            this.canBeModified = false;
            this.catchs = 0;
    }

}
