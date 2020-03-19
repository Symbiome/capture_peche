import AbstractFisholaService from '@/services/AbstractFisholaService';
import UserProfile from '@/pojos/UserProfile';
import TripsService from './TripsService';
import {UserSettings, Feedback} from '@/pojos/BackendPojos';

export default class ProfileService extends AbstractFisholaService {

    static settings?:UserSettings;

    constructor () {
        super();
    }

    static fetchProfile():Promise<UserProfile> {
        return new Promise<UserProfile>((resolve, reject) => {
            this.backendGet("/v1/security/profile")
                .then(
                    (fetched) => {
                        UserProfile.setCurrent(fetched);
                        resolve(fetched);
                    },
                    reject);
        });
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
        return new Promise<void>((resolve, reject) => {
            // XXX AThimel 19/02/2020 : Devrait être en POST
            this.backendGet("/v1/security/logout")
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
        return new Promise<UserSettings>((resolve, reject) => {
            this.backendGet("/v1/security/settings")
                .then(
                    (fetched) => {
                        ProfileService.settings = fetched;
                        resolve(fetched);
                    },
                    reject);
        });
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
                        delete ProfileService.settings;
                        resolve();
                    },
                    reject)
        });
    }

    static sendFeedback(feedback:Feedback):Promise<void> {
        return new Promise((resolve, reject) => {
            this.backendPut("/v1/feedback", feedback)
                .then(resolve, reject);
        });
    }

}
