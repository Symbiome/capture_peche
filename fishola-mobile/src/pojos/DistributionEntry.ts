
export default class DistributionEntry {
    constructor (
        public id:string,
        public name:string,
        public percent:number,
        public greenPercent:number,
        public count:number,
        public alias?:string
        ) {
    }
}
