import AbstractFisholaService from '@/services/AbstractFisholaService';
import UserProfile from '@/pojos/UserProfile';

export default class ProfileService extends AbstractFisholaService {

    constructor () {
        super();
    }

    static getProfile(callback:(profile:UserProfile)=>any, needLoginCallback:()=>any) {
        
        this.backendGet(
            "/v1/security/profile", 
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

}
