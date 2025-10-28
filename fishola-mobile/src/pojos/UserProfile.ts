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

export default class UserProfile {
  static currentUser?: UserProfile;

  id: string;
  lastName?: string;
  gender?: string;
  birthYear?: number;
  offlineMarker: boolean = false;

  constructor(
    public firstName: string,
    public email: string,
    public pseudo: string,
    public initials: string,
    public sampleBaseId: string,
    public acceptsMailNotifications: boolean,
    public acceptsShareTrips: boolean,
    public lastNewsSeenDate: Date
  ) {}

  static fullName(p: UserProfile) {
    let result = p.firstName;
    if (p.lastName) {
      result += " " + p.lastName;
    }
    return result;
  }

  static fromJson(input: any) {
    const result = new UserProfile(
      input.firstName,
      input.email,
      input.pseudo,
      input.initials,
      input.sampleBaseId,
      input.acceptsMailNotifications,
      input.acceptsShareTrips,
      input.lastNewsSeenDate,
    );
    result.lastName = input.lastName;
    result.gender = input.gender;
    result.birthYear = input.birthYear;
    return result;
  }

  static getCurrent(): UserProfile {
    return this.currentUser!;
  }

  static setCurrent(newProfile: UserProfile) {
    this.currentUser = newProfile;
  }

  static unsetCurrent() {
    delete this.currentUser;
  }
}
