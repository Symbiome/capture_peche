export default class UtilityServices {
  static parseLocalDateTime(someLocalDateTime: number[]): Date {
    if (someLocalDateTime[5]) {
      return new Date(
        someLocalDateTime[0],
        someLocalDateTime[1] - 1,
        someLocalDateTime[2],
        someLocalDateTime[3],
        someLocalDateTime[4],
        someLocalDateTime[5]
      );
    } else {
      return new Date(
        someLocalDateTime[0],
        someLocalDateTime[1] - 1,
        someLocalDateTime[2],
        someLocalDateTime[3],
        someLocalDateTime[4]
      );
    }
  }

  static formatDate(puet: number[]): string {
    const theDate: Date = UtilityServices.parseLocalDateTime(puet);
    const hourOptions: Intl.DateTimeFormatOptions = {
      month: "numeric",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "numeric"
    };
    const result: string = theDate.toLocaleTimeString("fr-FR", hourOptions);
    return result;
  }

  static camelCaseToUnderscore(cameCase: string) {
    return cameCase
      .replace(/\.?([A-Z])/g, function(x, y) {
        return "_" + y.toLowerCase();
      })
      .replace(/^_/, "");
  }
}
