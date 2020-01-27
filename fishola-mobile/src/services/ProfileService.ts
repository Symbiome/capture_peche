import AbstractFisholaService from '@/services/AbstractFisholaService';
import UserProfile from '@/pojos/UserProfile';

export default class ProfileService extends AbstractFisholaService {

    static instance?:ProfileService;

    constructor () {
        super();
    }

    static getInstance():ProfileService {
        if (!this.instance) {
            console.log("Pas encore d'instance partagée, on la créé");
            this.instance = new ProfileService();
        }
        return this.instance;
    }

    static getProfile(callback:(profile:UserProfile)=>any, needLoginCallback:()=>any) {
        
        this.getInstance().backendGet(
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
