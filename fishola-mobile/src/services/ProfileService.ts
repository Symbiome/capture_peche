import AbstractFisholaService from '@/services/AbstractFisholaService';
import UserProfile from '@/pojos/UserProfile';

export default class ProfileService extends AbstractFisholaService {

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
                        resolve();
                    },
                    reject);
        });
    }

}
