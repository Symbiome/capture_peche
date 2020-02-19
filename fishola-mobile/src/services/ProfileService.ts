import AbstractFisholaService from '@/services/AbstractFisholaService';
import UserProfile from '@/pojos/UserProfile';

export default class ProfileService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static getProfile(callback:(profile:UserProfile)=>any, needLoginCallback:()=>any) {
        this.backendGet("/v1/security/profile")
            .then(
                callback,
                (status:number) => {
                    if (status == 401) {
                        console.error("Need to login");
                        needLoginCallback();
                    } else {
                        console.error("C'est la merde noire");
                    }
                }
            );

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
