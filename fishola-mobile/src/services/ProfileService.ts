/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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
import AbstractFisholaService from '@/services/AbstractFisholaService';
import UserProfile from '@/pojos/UserProfile';
import TripsService from './TripsService';
import {UserSettings, Feedback, UpdatePasswordBean} from '@/pojos/BackendPojos';

export default class ProfileService extends AbstractFisholaService {

    static settings?:UserSettings;

    constructor () {
        super();
    }

    static fetchProfile():Promise<UserProfile> {
        return new Promise<UserProfile>((resolve, reject) => {
            this.backendGetOrOfflineStorage("/v1/security/profile")
                .then(
                    (fetched) => {
                        UserProfile.setCurrent(fetched);
                        resolve(fetched);
                    },
                    reject);
        });
    }

    static updatePassword(bean:UpdatePasswordBean):Promise<void> {
        return this.backendPost("/v1/security/password", bean);
    }

    static getProfile():Promise<UserProfile> {
        let profile:UserProfile = UserProfile.getCurrent();
        if (profile) {
            return Promise.resolve(profile);
        } else {
            return this.fetchProfile();
        }
    }

    static saveProfile(profile:UserProfile):Promise<void> {
        return new Promise((resolve, reject) => {
            this.backendPut("/v1/security/profile", profile)
                .then(
                    () => {
                        UserProfile.unsetCurrent();
                        resolve();
                    },
                    reject)
        });
    }

    static logout():Promise<void> {
        this.deleteFromOfflineStorage("/v1/security/profile");
        return new Promise<void>((resolve, reject) => {
            this.backendPost("/v1/security/logout")
                .then(
                    () => {
                        UserProfile.unsetCurrent();
                        TripsService.cancelCreations();
                        resolve();
                    },
                    reject);
        });
    }

    static fetchSettings():Promise<UserSettings> {
        let promise = new Promise<UserSettings>((resolve, reject) => {
            this.backendGetOrOfflineStorage("/v1/security/settings")
                .then(
                    (fetched) => {
                        ProfileService.settings = fetched;
                        resolve(fetched);
                    },
                    reject);
        });
        return promise;
    }

    static getSettings():Promise<UserSettings> {
        if (ProfileService.settings) {
            return Promise.resolve(ProfileService.settings);
        } else {
            return this.fetchSettings();
        }
    }

    static saveSettings(settings:UserSettings):Promise<void> {
        return new Promise((resolve, reject) => {
            this.backendPut("/v1/security/settings", settings)
                .then(
                    () => {
                        this.deleteFromOfflineStorage("/v1/security/settings");
                        delete ProfileService.settings;
                        resolve();
                    },
                    reject)
        });
    }

    static prepareCaches():Promise<void> {
        let allPromises:Promise<void>[] = [
            this.prepareCache('/v1/security/settings')
        ];
        return new Promise<void>((resolve, reject) => {
            Promise.all(allPromises).then(
                () => resolve()
                ,reject
            );
        });
    }

    static sendFeedback(feedback:Feedback):Promise<void> {
        return new Promise((resolve, reject) => {
            this.backendPut("/v1/feedback", feedback)
                .then(resolve, reject);
        });
    }

}
