
import moment from 'moment';

export default class Helpers {

    static computeDurationInSeconds(startedAt:string, finishedAt?:string):number {
        let start = moment(startedAt, moment.HTML5_FMT.TIME_SECONDS);
        let end = moment();
        if (finishedAt) {
            end = moment(finishedAt, moment.HTML5_FMT.TIME_SECONDS);
        }
        if (end.isBefore(start)) {
            end = end.add(24, 'hours');
        }
        let seconds = end.diff(start, 'seconds');
        return seconds;
    }

    static computeDurationFromString(startedAt:string, finishedAt:string):string {
        let seconds = this.computeDurationInSeconds(startedAt, finishedAt);
        let result = Helpers.computeDurationFromSeconds(seconds);
        return result;
    }

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
        let days = Math.floor(hours/24);
        let result = '';
        if (days > 0) {
            result += days + 'd ';
            hours -= days * 24;
        }
        if (hours > 0) {
            result += hours + 'h ';
            minutes -= days * 24*60 + hours * 60;
        }
        if (minutes > 0) {
            result += minutes + 'min ';
            seconds -= days * 24*60*60 + hours * 60*60 + minutes * 60;
        }
        return result;
    }

    static computeDurationTrunced(startedAt:Date, finishedAt:Date):string {
        let seconds = Math.floor((finishedAt.getTime() - startedAt.getTime())/1000);
        let result = Helpers.computeDurationTruncedFromSeconds(seconds);
        return result;
    }

    static computeDurationTruncedFromSeconds(seconds:number):string {

        if (seconds < 60) {
            return seconds + 's';
        }

        let minutes = Math.floor(seconds/60);
        let hours = Math.floor(minutes/60);
        let days = Math.floor(hours/24);
        let result = '';
        if (days > 0) {
            result += days + 'd ';
            hours -= days * 24;
        }
        if (hours > 0) {
            result += hours + 'h ';
            minutes -= days * 24*60 + hours * 60;
        }
        if (minutes > 0 && days == 0) {
            result += minutes + 'min ';
            seconds -= days * 24*60*60 + hours * 60*60 + minutes * 60;
        }
        return result;
    }

    static formatToLongDate(date:Date):string {
        var dayOptions = {weekday: "long", month: "long", day: "numeric", year: "numeric"};
        let result = date.toLocaleDateString('fr-FR', dayOptions);
        return result;
    }

    static formatToDate(date:Date):string {
        let year = date.getFullYear();
        let month = date.getMonth()+1;
        let day = date.getDate();
        let result = year + '-' + (month < 10 ? '0' : '') + month + '-' + (day < 10 ? '0' : '') + day;
        return result;
    }

    // static formatToTime(time:Date):string {
    //     let hours = time.getHours();
    //     let minutes = time.getMinutes();
    //     let result = (hours < 10 ? '0' : '') + hours + ':' + (minutes < 10 ? '0' : '') + minutes;
    //     return result;
    // }

    static parseDateTime(date:Date, time:string):Date {
        let result = new Date(date);
        let hour = parseInt(time.substring(0, 2));
        let minute = parseInt(time.substring(3));
        result.setHours(hour, minute);
        return result;
    }

    static parseLocalDate(someLocalDateTime:number[]):Date {
        let result:Date = new Date(
            someLocalDateTime[0],
            someLocalDateTime[1] - 1,
            someLocalDateTime[2],
        );
        return result;
    }

    static parseLocalDateTime(someLocalDateTime:number[]):Date {
        let result:Date = new Date(
            someLocalDateTime[0],
            someLocalDateTime[1] - 1,
            someLocalDateTime[2],
            someLocalDateTime[3],
            someLocalDateTime[4],
            someLocalDateTime[5],
        );
        return result;
    }

    static truncateTimeToMinutes(input:string):string {
        if (!input) {
            return input;
        }
        let m = moment(input, moment.HTML5_FMT.TIME_SECONDS);
        let result = m.format('HH:mm');
        return result;
    }
}