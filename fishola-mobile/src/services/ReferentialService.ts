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
import {WaterEntity as Lake, Weather, SpeciesWithAlias, Technique, ReleasedFishState, AttributionResponse, NearbyWaterEntity} from '@/pojos/BackendPojos';
import AbstractFisholaService from '@/services/AbstractFisholaService';

export class SpeciesWithAliasAndTechnique {
    constructor (
        public species:SpeciesWithAlias[],
        public techniques:Technique[]) {
    }
}

export class WeathersTripTypesSpeciesAndTechniques {
    constructor (
        public weathers:Weather[],
        public tripTypes:any[],
        public species:Map<string, SpeciesWithAlias[]>,
        public techniques:Technique[]) {
    }
}

export default class ReferentialService extends AbstractFisholaService {
  constructor() {
    super();
  }

  // Listing léger (sans géométrie) : le référentiel complet sérialise la
  // géométrie de chaque entité, ~1,2 Go pour le réseau France entière — bien
  // au-delà du timeout de 5 s de backendGetWithCache, ce qui faisait échouer
  // ce chargement (et en cascade les formulaires de saisie qui en dépendent :
  // plan d'eau, type de pêche). Ce formulaire n'a besoin que de
  // nom/type/centroïde ; la géométrie fine reste servie par les tuiles MVT.
  static getLakes(): Promise<Lake[]> {
    return this.backendGetWithCache("/v1/referential/waterEntities/summary");
  }

  static getFavoriteLakes(): Promise<Lake[]> {
    return this.backendGetWithCache("/v1/referential/waterEntities/favorites");
  }

  // Mémoïsé sur la référence du tableau source (~181k entités, cf. #128) :
  // sans ça, chaque appelant (une carte de sortie par élément de liste, ex.
  // MyTripsItem) reconstruisait sa propre Map nationale rien que pour
  // résoudre un seul nom de lac par id.
  private static lakesIndexCache: { source: Lake[]; index: Map<string, Lake> } | null = null;

  static getLakesIndex(): Promise<Map<string, Lake>> {
    return ReferentialService.getLakes().then((lakes: Lake[]) => {
      const cache = ReferentialService.lakesIndexCache;
      if (cache && cache.source === lakes) {
        return cache.index;
      }
      const index = new Map<string, Lake>();
      lakes.forEach((lake: Lake) => index.set(lake.id, lake));
      ReferentialService.lakesIndexCache = { source: lakes, index };
      return index;
    });
  }

  // Recherche serveur des entités hydro (insensible casse/accents, tolérante aux
  // fautes) — remplace le filtrage en mémoire de la liste complète. Les résultats
  // sont normalisés en Lake (= WaterEntity) pour rester compatibles avec les
  // consommateurs existants ; seuls id/name/kind/latitude/longitude sont
  // renseignés (le centroïde sert au centrage carte).
  static searchWaterEntities(q: string): Promise<Lake[]> {
    return this.backendGet(`/v1/waterEntities/search?q=${encodeURIComponent(q)}`)
      .then((results: any[]) => (results || []).map((r) => ({
        id: r.waterEntityId,
        name: r.name,
        kind: r.kind,
        latitude: r.centroid ? r.centroid.lat : undefined,
        longitude: r.centroid ? r.centroid.lng : undefined,
        // Commune + code postal (#6/#15) — désambiguïsation dans l'autocomplete.
        commune: r.commune,
        codePostal: r.codePostal,
        exportAs: r.name,
        waterEntityCode: "",
        nature: "",
        altitudeMoyenne: 0,
        bdtopoCleabs: "",
        geom: "",
      } as unknown as Lake)));
  }

  // Entités hydro autour d'un point, triées par distance (#5). Alimente le mode
  // liste « autour de moi » : géoloc/pin → tri par distance, filtre kind,
  // pagination. Le tri est fait côté serveur (PostGIS ST_DWithin + ORDER BY dist).
  static getNearby(
    lat: number,
    lng: number,
    radiusM = 2000,
    kind?: string,
    pageNumber = 0,
    pageSize = 20
  ): Promise<NearbyWaterEntity[]> {
    let url = `/v1/waterEntities/nearby?lat=${lat}&lng=${lng}`
      + `&radiusM=${radiusM}&pageNumber=${pageNumber}&pageSize=${pageSize}`;
    if (kind) {
      url += `&kind=${encodeURIComponent(kind)}`;
    }
    return this.backendGet(url).then((results: any[]) => results || []);
  }

  // Résout la commune + code postal d'une entité par son id (#15). Utilisé
  // quand l'entité est choisie hors recherche (tap carte, mode liste) : les
  // objets du référentiel complet ne portent pas la commune. Silencieux en cas
  // de 404 / hors-ligne (on n'affiche simplement pas la commune).
  static getWaterEntityCommune(
    id: string
  ): Promise<{ commune?: string; codePostal?: string } | null> {
    return this.backendGet(`/v1/waterEntities/${encodeURIComponent(id)}`)
      .then((r: any) => (r ? { commune: r.commune, codePostal: r.codePostal } : null))
      .catch(() => null);
  }

  // Recherche serveur des communes (référentiel ADMIN EXPRESS, #6).
  static searchCommunes(q: string): Promise<any[]> {
    return this.backendGet(`/v1/communes/search?q=${encodeURIComponent(q)}`);
  }

  // Entités hydro d'une commune (ou dans un buffer de sa limite), triées par
  // distance au centroïde communal (#6). Alimente la recherche « par commune ».
  static getWaterEntitiesByCommune(
    insee: string,
    bufferM = 500
  ): Promise<NearbyWaterEntity[]> {
    return this.backendGet(
      `/v1/waterEntities/byCommune?insee=${encodeURIComponent(insee)}&bufferM=${bufferM}`
    ).then((results: any[]) => results || []);
  }

  // Proposition d'attribution hydro d'un point (#9) : entité la plus proche +
  // alternatives, pour le flux « pin sur la carte → confirmation ».
  static getAttribution(lat: number, lng: number): Promise<AttributionResponse> {
    return this.backendGet(`/v1/waterEntities/attribution?lat=${lat}&lng=${lng}`);
  }

  static getSpeciesPerLake(): Promise<Map<string, SpeciesWithAlias[]>> {
    return new Promise<Map<string, SpeciesWithAlias[]>>((resolve, reject) => {
      // Repli hors ligne obligatoire : ce référentiel est consulté pendant la
      // validation d'une capture (contrôle de la taille maximale). Avec un
      // simple `backendGet`, la promesse rejetait sans réseau et la validation
      // était abandonnée en silence — capture impossible à saisir hors ligne.
      this.backendGetOrOfflineStorage("/v1/referential/species-per-waterEntity").then((map) => {
        const someMap = new Map<string, SpeciesWithAlias[]>();
        const lakeIds: string[] = Object.keys(map);
        lakeIds.forEach((lakeId) => {
          someMap.set(lakeId, map[lakeId]);
        });
        resolve(someMap);
      }, reject);
    });
  }

  static getSpeciesWithoutLake(): Promise<SpeciesWithAlias[]> {
    return this.backendGetWithCache("/v1/referential/species");
  }

  static getSpeciesCustom(): Promise<SpeciesWithAlias[]> {
    return this.backendGetWithCache("/v1/referential/species-custom");
  }

  static getSpeciesCustomNoCache(): Promise<SpeciesWithAlias[]> {
    return this.backendGet("/v1/referential/species-custom");
  }

  static getAllSpecies(): Promise<SpeciesWithAlias[]> {
    return new Promise<SpeciesWithAlias[]>((resolve, reject) => {
      Promise.all([
        ReferentialService.getSpeciesWithoutLake(),
        ReferentialService.getSpeciesCustom(),
      ]).then((data: [SpeciesWithAlias[], SpeciesWithAlias[]]) => {
        const result: SpeciesWithAlias[] = [];

        data[0].forEach((value) => result.push(value));
        data[1].forEach((value) => result.push(value));

        resolve(result);
      }, reject);
    });
  }

  static getAllSpeciesNoCache(): Promise<SpeciesWithAlias[]> {
    return new Promise<SpeciesWithAlias[]>((resolve, reject) => {
      Promise.all([
        ReferentialService.getSpeciesWithoutLake(),
        ReferentialService.getSpeciesCustomNoCache(),
      ]).then((data: [SpeciesWithAlias[], SpeciesWithAlias[]]) => {
        const result: SpeciesWithAlias[] = [];

        data[0].forEach((value) => result.push(value));
        data[1].forEach((value) => result.push(value));

        resolve(result);
      }, reject);
    });
  }

  static clearSpeciesCustomCache() {
    this.clearCache("/v1/referential/species-custom");
  }

  static getSpeciesPerLakePlusCustom(): Promise<
    Map<string, SpeciesWithAlias[]>
  > {
    return new Promise<Map<string, SpeciesWithAlias[]>>((resolve, reject) => {
      Promise.all([
        ReferentialService.getSpeciesPerLake(),
        ReferentialService.getSpeciesCustom(),
      ]).then(
        (
          serverResponse: [Map<string, SpeciesWithAlias[]>, SpeciesWithAlias[]]
        ) => {
          resolve(ReferentialService.buildSpeciesAliasMapFromServerResponse(serverResponse));
        },
        reject
      );
    });
  }

  static buildSpeciesAliasMapFromServerResponse(
    serverResponse: [Map<string, SpeciesWithAlias[]>, SpeciesWithAlias[]]
  ) {
    const result: Map<string, SpeciesWithAlias[]> = new Map<
      string,
      SpeciesWithAlias[]
    >();

    const custom: SpeciesWithAlias[] = serverResponse[1];

    const perLake = serverResponse[0];
    perLake.forEach((value, lakeId) => {
      if (lakeId != "offlineMarker") {
        const lakeSpecies: SpeciesWithAlias[] = [];
        value.forEach((s) => lakeSpecies.push(s));
        // Quel que soit le lac, on ajoute les espèces custom à la liste
        custom.forEach((s) => lakeSpecies.push(s));
        result.set(lakeId, lakeSpecies);
      }
    });
    return result;
  }

  static getSpecies(lakeId: string): Promise<SpeciesWithAlias[]> {
    return new Promise<SpeciesWithAlias[]>((resolve, reject) => {
      this.getSpeciesPerLake().then((map) => {
        const species = map.get(lakeId);
        if (species) {
          resolve(species);
        } else {
          resolve([]);
        }
      }, reject);
    });
  }

  static getSpeciesPlusCustom(lakeId: string): Promise<SpeciesWithAlias[]> {
    return new Promise<SpeciesWithAlias[]>((resolve, reject) => {
      this.getSpeciesPerLakePlusCustom().then((map) => {
        const species = map.get(lakeId);
        if (species) {
          resolve(species);
        } else {
          resolve([]);
        }
      }, reject);
    });
  }

  static getWeathers(): Promise<Weather[]> {
    return this.backendGetWithCache("/v1/referential/weathers");
  }

  static getTechniques(): Promise<Technique[]> {
    return this.backendGetWithCache("/v1/referential/techniques");
  }

  static getTechniquesIndex(): Promise<Map<string, Technique>> {
    return new Promise<Map<string, Technique>>((resolve, reject) => {
      ReferentialService.getTechniques().then((techniques: Technique[]) => {
        const result = new Map<string, Technique>();
        techniques.forEach((t: Technique) => {
          result.set(t.id, t);
        });
        resolve(result);
      }, reject);
    });
  }

  static getReleasedFishStates(): Promise<ReleasedFishState[]> {
    return this.backendGetWithCache("/v1/referential/released-fish-states");
  }

  static getTripTypes(): Promise<any[]> {
    const types = [
      { id: "Border", name: "Pêche du bord" },
      { id: "Craft", name: "Depuis une embarcation" },
    ];
    return Promise.resolve(types);
  }

  // Ne charge PLUS le référentiel national des lacs (cf. #128) : le plan
  // d'eau se sélectionne exclusivement via l'autocomplete serveur
  // (searchWaterEntities) dans LakeSelection, qui n'a pas besoin de cette
  // liste. Le lac déjà associé à une sortie existante se résout séparément
  // via getLakesIndex() (mémoïsée).
  static getWeathersTripTypesSpeciesAndTechniques(): Promise<WeathersTripTypesSpeciesAndTechniques> {
    return new Promise<WeathersTripTypesSpeciesAndTechniques>(
      (resolve, reject) => {
        Promise.all([
          ReferentialService.getWeathers(),
          ReferentialService.getTripTypes(),
          ReferentialService.getSpeciesPerLakePlusCustom(),
          ReferentialService.getTechniques(),
        ]).then(
          (
            data: [
              Weather[],
              any[],
              Map<string, SpeciesWithAlias[]>,
              Technique[]
            ]
          ) => {
            const result: WeathersTripTypesSpeciesAndTechniques =
              new WeathersTripTypesSpeciesAndTechniques(
                data[0],
                data[1],
                data[2],
                data[3]
              );
            resolve(result);
          },
          reject
        );
      }
    );
  }

  static getSpeciesAndTechniques(
    lakeId?: string
  ): Promise<SpeciesWithAliasAndTechnique> {
    return new Promise<SpeciesWithAliasAndTechnique>((resolve, reject) => {
      const speciesPromise = lakeId
        ? ReferentialService.getSpeciesPlusCustom(lakeId)
        : ReferentialService.getAllSpecies();
      Promise.all([speciesPromise, ReferentialService.getTechniques()]).then(
        (data: [SpeciesWithAlias[], Technique[]]) => {
          const result: SpeciesWithAliasAndTechnique =
            new SpeciesWithAliasAndTechnique(data[0], data[1]);
          resolve(result);
        },
        reject
      );
    });
  }

  static prepareCaches(): Promise<void> {
    const allPromises: Promise<void>[] = [
      this.prepareCache("/v1/referential/waterEntities"),
      // Les favoris sont lus par le sélecteur de plan d'eau : sans cette mise
      // en cache, un appareil qui n'a jamais ouvert l'écran en ligne n'a aucune
      // entrée locale et le sélecteur restait vide hors ligne.
      this.prepareCache("/v1/referential/waterEntities/favorites"),
      this.prepareCache("/v1/referential/species-per-waterEntity"),
      this.prepareCache("/v1/referential/species"),
      this.prepareCache("/v1/referential/species-custom"),
      this.prepareCache("/v1/referential/weathers"),
      this.prepareCache("/v1/referential/techniques"),
      this.prepareCache("/v1/referential/released-fish-states"),
    ];
    return new Promise<void>((resolve, reject) => {
      Promise.all(allPromises).then(() => resolve(), reject);
    });
  }
}
