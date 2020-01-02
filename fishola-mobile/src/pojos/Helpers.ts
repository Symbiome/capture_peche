
export default class Helpers {

    static computeDuration(startedAt:Date, finishedAt:Date):string {
        let seconds = Math.floor((finishedAt.getTime()-startedAt.getTime())/1000);
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

}