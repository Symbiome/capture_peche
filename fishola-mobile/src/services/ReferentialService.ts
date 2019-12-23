import Lake from '@/pojos/Lake';
import Weather from '@/pojos/Weather';
import Species from '@/pojos/Species';
import AbstractFisholaService from '@/services/AbstractFisholaService';

export default class ReferentialService extends AbstractFisholaService {

    static instance?:ReferentialService;

    constructor () {
        super();
    }

    static getInstance():ReferentialService {
        if (!this.instance) {
            console.log("Pas encore d'instance partagée, on la créé");
            this.instance = new ReferentialService();
        }
        return this.instance;
    }

    static getLakes(callback:(lakes:Lake[])=>any) {
        this.getInstance().backendGet('/v1/referential/lakes', callback);
    }

    static getWeathers(callback:(weathers:Weather[])=>any) {
        this.getInstance().backendGet('/v1/referential/weathers', callback);
    }

    static getSpecies(lakeId:string, callback:(resul:Species[])=>any) {
        this.getInstance().backendGet('/v1/referential/species-per-lake', (map) => {
            let species = map[lakeId];
            callback(species);
        });
    }

    static getTripTypes(callback:(result:any[])=>any) {
        let types = [
            {id: 'Border', name: 'Pêche du bord'},
            {id: 'Craft', name: 'Depuis une embarcation'}
        ];
        callback(types);
    }

}
