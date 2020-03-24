
import moment from 'moment';

export default class Helpers {

    static renderDuration(startedAt:string, finishedAt?:string):string {
        let duration = this.computeDuration(startedAt, finishedAt);
        let result = this.formatDuration(duration);
        return result;
    }

    static humanizeDuration(startedAt:string, finishedAt?:string):string {
        let duration = this.computeDuration(startedAt, finishedAt);
        let result = duration.humanize();
        return result;
    }

    static humanizeDurationFromDates(startedAt:Date, finishedAt:Date):string {
        let duration = this.computeDurationFromDates(startedAt, finishedAt);
        let result = duration.humanize();
        return result;
    }

    static computeDuration(startedAt:string, finishedAt?:string):moment.Duration {
        let start = moment(startedAt, moment.HTML5_FMT.TIME_SECONDS);
        let end = moment();
        if (finishedAt) {
            end = moment(finishedAt, moment.HTML5_FMT.TIME_SECONDS);
        }
        if (end.isBefore(start)) {
            end = end.add(24, 'hours');
        }
        let diff = end.diff(start);
        let result = moment.duration(diff)
        return result;
    }

    static computeDurationFromDates(startedAt:Date, finishedAt:Date):moment.Duration {
        let start = moment(startedAt);
        let end = moment(finishedAt);
        if (end.isBefore(start)) {
            end = end.add(24, 'hours');
        }
        let diff = end.diff(start);
        let result = moment.duration(diff)
        return result;
    }

    static formatDuration(duration:moment.Duration):string {

        let result = '';
        if (duration.days() > 0) {
            result += duration.days() + 'd ';
        }
        if (duration.hours() > 0) {
            result += duration.hours() + 'h ';
        }
        if (duration.minutes() > 0) {
            result += duration.minutes() + 'min ';
        }
        if (duration.seconds() > 0) {
            result += duration.seconds() + 's ';
        }

        if (result == '') {
            result = '0s';
        }

        return result;
    }

    static formatSecondsDuration(seconds:number):string {
        let duration:moment.Duration = moment.duration(seconds, 'seconds');
        let result = this.formatDuration(duration);
        return result;
    }

    static computeDurationInSeconds(startedAt:string, finishedAt?:string):number {
        let duration:moment.Duration = this.computeDuration(startedAt, finishedAt);
        let seconds = Math.floor(duration.asSeconds());
        return seconds;
    }

    static formatSecondsDurationTruncate(seconds:number):string {

        if (seconds < 60) {
            return seconds + 's';
        }

        let duration:moment.Duration = moment.duration(seconds, 'seconds');

        // On tronque à la minute inférieur
        duration.subtract(duration.seconds(), 'seconds');

        let result = this.formatDuration(duration);
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