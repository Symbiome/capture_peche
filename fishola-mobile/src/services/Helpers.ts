/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import { DeviceType } from "@/pojos/BackendPojos";

import { Device } from "@capacitor/device";

import moment from "moment";

export default class Helpers {
  static renderDuration(startedAt: string, finishedAt?: string): string {
    const duration = this.computeDuration(startedAt, finishedAt);
    const result = this.formatDuration(duration, true);
    return result;
  }

  static renderDurationNoSeconds(
    startedAt: string,
    finishedAt?: string
  ): string {
    const duration = this.computeDuration(startedAt, finishedAt);
    const result = this.formatDuration(duration, false);
    return result;
  }

  static humanizeDuration(startedAt: string, finishedAt?: string): string {
    const duration = this.computeDuration(startedAt, finishedAt);
    const result = duration.humanize();
    return result;
  }

  static humanizeDurationFromDates(startedAt: Date, finishedAt: Date): string {
    const duration = this.computeDurationFromDates(startedAt, finishedAt);
    const result = duration.humanize();
    return result;
  }

  static computeDuration(
    startedAt: string,
    finishedAt?: string
  ): moment.Duration {
    const start = moment(startedAt, moment.HTML5_FMT.TIME_SECONDS);
    let end = moment();
    if (finishedAt) {
      end = moment(finishedAt, moment.HTML5_FMT.TIME_SECONDS);
    }
    const diff = end.diff(start);
    const result = moment.duration(diff);
    return result;
  }

  static computeDurationFromDates(
    startedAt: Date,
    finishedAt: Date
  ): moment.Duration {
    const start = moment(startedAt);
    const end = moment(finishedAt);
    const diff = end.diff(start);
    const result = moment.duration(diff);
    return result;
  }

  static formatDuration(
    duration: moment.Duration,
    includeSeconds?: boolean
  ): string {
    let result = "";
    if (duration.days() > 0) {
      result += duration.days() + "d ";
    }
    if (duration.hours() > 0) {
      result += duration.hours() + "h ";
    }
    if (duration.minutes() > 0) {
      result += duration.minutes() + "min ";
    }
    if (includeSeconds && duration.seconds() > 0) {
      result += duration.seconds() + "s ";
    }

    if (result == "") {
      result = "0" + (includeSeconds ? "s" : "min");
    }

    return result;
  }

  static formatSecondsDuration(seconds: number): string {
    const duration: moment.Duration = moment.duration(seconds, "seconds");
    const result = this.formatDuration(duration);
    return result;
  }

  static computeDurationInSeconds(
    startedAt: string,
    finishedAt?: string
  ): number {
    const duration: moment.Duration = this.computeDuration(
      startedAt,
      finishedAt
    );
    const seconds = Math.floor(duration.asSeconds());
    return seconds;
  }

  static formatSecondsDurationTruncate(seconds: number): string {
    if (seconds < 60) {
      return seconds + "s";
    }

    const duration: moment.Duration = moment.duration(seconds, "seconds");

    // On tronque à la minute inférieur
    duration.subtract(duration.seconds(), "seconds");

    const result = this.formatDuration(duration);
    return result;
  }

  static formatToDateWithoutYear(date: Date): string {
    const dayOptions: Intl.DateTimeFormatOptions = {
      day: "numeric",
      month: "long",
    };
    const result = date.toLocaleDateString("fr-FR", dayOptions);
    return result;
  }
  static formatToLongDate(date: Date): string {
    const dayOptions: Intl.DateTimeFormatOptions = {
      weekday: "long",
      month: "long",
      day: "numeric",
      year: "numeric",
    };
    const result = date.toLocaleDateString("fr-FR", dayOptions);
    return result;
  }

  static formatToDate(date: Date): string {
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const result =
      year +
      "-" +
      (month < 10 ? "0" : "") +
      month +
      "-" +
      (day < 10 ? "0" : "") +
      day;
    return result;
  }

  static formatToHour(date: Date): string {
    const hours = date.getHours();
    const minutes = date.getMinutes();
    const result =
      (hours < 10 ? "0" : "") +
      hours +
      "h" +
      (minutes < 10 ? "0" : "") +
      minutes;
    return result;
  }

  static parseDateTime(date: Date, time: string): Date {
    const result = new Date(date);
    const hour = parseInt(time.substring(0, 2));
    const minute = parseInt(time.substring(3));
    result.setHours(hour, minute);
    return result;
  }

  static parseLocalDate(someLocalDateTime: number[]): Date {
    const result: Date = new Date(
      someLocalDateTime[0],
      someLocalDateTime[1] - 1,
      someLocalDateTime[2]
    );
    return result;
  }

  static parseLocalDateTime(someLocalDateTime: number[]): Date {
    if (someLocalDateTime.length == 5) {
      return new Date(
        someLocalDateTime[0],
        someLocalDateTime[1] - 1,
        someLocalDateTime[2],
        someLocalDateTime[3],
        someLocalDateTime[4],
        0
      );
    } else {
      return new Date(
        someLocalDateTime[0],
        someLocalDateTime[1] - 1,
        someLocalDateTime[2],
        someLocalDateTime[3],
        someLocalDateTime[4],
        someLocalDateTime[5]
      );
    }
  }

  static truncateTimeToMinutes(input: string): string {
    if (!input) {
      return input;
    }
    const m = moment(input, moment.HTML5_FMT.TIME_SECONDS);
    const result = m.format("HH:mm");
    return result;
  }

  static confirm(
    modal: any,
    text: string,
    title?: string,
    rejectText?: string,
    resolveText?: string
  ): Promise<void> {
    return new Promise<void>((resolve, reject) => {
      const params: any = {
        title: title,
        text: text,
        buttons: [
          {
            title: rejectText || "Non",
            handler: () => {
              modal.hide("dialog");
              reject();
            },
          },
          {
            title: resolveText || "Oui",
            handler: () => {
              modal.hide("dialog");
              resolve();
            },
          },
        ],
      };
      modal.show("dialog", params);
    });
  }

  static alert(modal: any, text: string, title?: string): Promise<void> {
    return new Promise<void>((resolve, _reject) => {
      const params: any = {
        title: title,
        text: text,
        buttons: [
          {
            title: "J'ai compris",
            handler: () => {
              modal.hide("dialog");
              resolve();
            },
          },
        ],
      };
      modal.show("dialog", params);
    });
  }

  static getDeviceType(): Promise<DeviceType> {
    if (process.env.VUE_APP_FORCE_DEVICE_TYPE) {
      console.warn(
        "Device type is forced to",
        process.env.VUE_APP_FORCE_DEVICE_TYPE
      );
      return Promise.resolve(process.env.VUE_APP_FORCE_DEVICE_TYPE);
    }

    return new Promise<DeviceType>((resolve, reject) => {
      Device.getInfo().then((info) => {
        let source: DeviceType;
        if (info.platform == "web") {
          source = "web";
        } else {
          source = "application";
        }
        resolve(source);
      }, reject);
    });
  }

  static ifApplication(callback: () => any) {
    this.getDeviceType().then((type) => {
      if (type == "application") {
        callback();
      }
    });
  }

  static ifWeb(callback: () => any) {
    this.getDeviceType().then((type) => {
      if (type == "web") {
        callback();
      }
    });
  }
}
