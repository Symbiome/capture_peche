
export default class Helpers {

    static computeDuration(startedAt:Date, finishedAt:Date):string {
        let seconds = Math.floor((finishedAt.getTime() - startedAt.getTime())/1000);
        let result = Helpers.computeDurationFromSeconds(seconds);
        return result;
    }

    static computeDurationFromSeconds(seconds:number):string {

        if (seconds < 60) {
            return seconds + 's';
        }

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
        return result;
    }

    static formateToDate(date:Date):string {
        let year = date.getFullYear();
        let month = date.getMonth()+1;
        let day = date.getDate();
        let result = year + '-' + (month < 10 ? '0' : '') + month + '-' + (day < 10 ? '0' : '') + day;
        return result;
    }

    static formateToTime(time:Date):string {
        let hours = time.getHours();
        let minutes = time.getMinutes();
        let result = (hours < 10 ? '0' : '') + hours + ':' + (minutes < 10 ? '0' : '') + minutes;
        return result;
    }

    static parseDateTime(date:string, time:string):Date {
        let result = new Date(date);
        let hour = parseInt(time.substring(0, 2));
        let minute = parseInt(time.substring(3));
        result.setHours(hour, minute);
        return result;
    }

}