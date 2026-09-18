<!--
  #%L
  Fishola :: Mobile
  %%
  Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
  %%
  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #L%
  -->
<!--
  Onglet « Carte » de l'écran Zones hors-ligne (#180) : visualise l'étendue
  géographique de tous les packs hydro départementaux téléchargés (#54), sans
  connexion. Réutilise le fond de carte partagé (createFisholaMap,
  maplibreStyle.ts) ; le fond IGN ne se charge pas hors-ligne (packs =
  entités hydro uniquement, pas de tuiles), la géométrie téléchargée reste
  visible. Aucun appel réseau : les données viennent du prop `packs`, déjà
  chargé localement par la vue parente (OfflineAreasService.listPacks()).
  -->
<template>
    <div class="offline-packs-map">
        <p v-if="!packs.length" class="empty-map">
            Aucun département téléchargé pour le moment. Téléchargez un
            département ci-dessus pour visualiser sa couverture sur la carte.
        </p>
        <template v-else>
            <div ref="mapContainer" class="opm-container" />
            <p class="opm-note" v-if="offline">
                <i class="icon-error" /> Fond de carte non disponible
                hors-ligne : seul le réseau téléchargé (ci-dessous) est
                affiché.
            </p>
        </template>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import maplibregl, { Map as MlMap, Marker, GeoJSONSource, LngLatBoundsLike } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { attachHydroHover, createFisholaMap } from '@/components/common/maplibreStyle';
import OfflineArea from '@/pojos/OfflineArea';

interface DepartmentLabel {
    code: string;
    name: string;
    center: [number, number];
}

@Component
export default class OfflinePacksMap extends Vue {
    @Prop({ default: () => [] }) packs: OfflineArea[];
    @Prop({ default: false }) offline: boolean;

    private map: MlMap | null = null;
    private labelMarkers: Marker[] = [];
    private detachHydroHover: (() => void) | null = null;

    // Le composant n'est monté que le temps où l'onglet Carte est ouvert
    // (OfflineAreas.vue, v-if/v-else) : pas de notion de visibilité à gérer
    // ici, juste une carte fraîche à chaque ouverture (coût négligeable,
    // données déjà en mémoire).
    mounted() {
        if (this.packs.length) {
            this.$nextTick(() => this.initOrUpdate());
        }
    }

    beforeDestroy() {
        this.destroyMap();
    }

    // Ajout/suppression d'un pack pendant que l'onglet est ouvert (#180) : la
    // carte (re)construit sa source et se recadre sur la nouvelle emprise.
    @Watch('packs')
    onPacksChange() {
        if (!this.packs.length) {
            this.destroyMap();
            return;
        }
        this.$nextTick(() => this.initOrUpdate());
    }

    private destroyMap() {
        this.clearLabels();
        this.detachHydroHover?.();
        this.detachHydroHover = null;
        this.map?.remove();
        this.map = null;
    }

    private initOrUpdate() {
        const { collection, bounds, departments } = this.buildTaggedCollection();
        if (!this.map) {
            const container = this.$refs.mapContainer as HTMLElement;
            if (!container) {
                return;
            }
            this.map = createFisholaMap(container, {});
            this.map.on('load', () => {
                if (!this.map) {
                    return;
                }
                this.map.addSource('hydro-offline', { type: 'geojson', data: collection });
                // Mêmes identifiants de couche que MapLibreMap.vue : attachHydroHover
                // (ciblage mutualisé) reconnaît hydro-offline-fill/-line et affiche
                // l'infobulle nom/type d'une entité au survol (desktop).
                this.map.addLayer({
                    id: 'hydro-offline-fill',
                    type: 'fill',
                    source: 'hydro-offline',
                    filter: ['==', ['get', 'kind'], 'STILL'],
                    paint: { 'fill-color': '#1e9bc4', 'fill-opacity': 0.35, 'fill-outline-color': '#1478a0' },
                });
                this.map.addLayer({
                    id: 'hydro-offline-line',
                    type: 'line',
                    source: 'hydro-offline',
                    paint: {
                        'line-color': '#1e9bc4',
                        'line-width': ['interpolate', ['linear'], ['zoom'], 6, 1, 14, 3],
                    },
                });
                this.detachHydroHover = attachHydroHover(this.map);
                this.renderLabels(departments);
                this.fitToBounds(bounds);
            });
        } else {
            const source = this.map.getSource('hydro-offline') as GeoJSONSource | undefined;
            source?.setData(collection);
            this.renderLabels(departments);
            this.fitToBounds(bounds);
        }
    }

    private fitToBounds(bounds: LngLatBoundsLike | null) {
        if (!this.map || !bounds) {
            return;
        }
        this.map.fitBounds(bounds, { padding: 50, maxZoom: 12 });
    }

    // Un marqueur DOM (texte natif) par département, positionné au centre de
    // sa propre emprise : un `symbol`/`text-field` MapLibre nécessite une
    // source de glyphes (réseau, cf. #180 notes techniques -- aucune n'est
    // configurée dans ce style et il n'y en a pas dans le reste de l'appli),
    // incompatible avec l'affichage hors-ligne. Même pattern que les
    // marqueurs favoris/position de MapLibreMap.vue.
    private renderLabels(departments: DepartmentLabel[]) {
        this.clearLabels();
        if (!this.map) {
            return;
        }
        departments.forEach((d) => {
            const el = document.createElement('div');
            el.className = 'department-label';
            el.textContent = d.name;
            const marker = new maplibregl.Marker({ element: el, anchor: 'center' })
                .setLngLat(d.center)
                .addTo(this.map!);
            this.labelMarkers.push(marker);
        });
    }

    private clearLabels() {
        this.labelMarkers.forEach((m) => m.remove());
        this.labelMarkers = [];
    }

    // Fusionne tous les packs en un FeatureCollection, chaque feature tagguée
    // de son département (department_code/department_name, absents du pack
    // brut) pour distinguer visuellement leur couverture (#180) ; calcule
    // aussi l'emprise globale (recadrage) et le point d'ancrage de chaque
    // étiquette de département (centre de sa propre emprise).
    private buildTaggedCollection(): {
        collection: any;
        bounds: LngLatBoundsLike | null;
        departments: DepartmentLabel[];
    } {
        const features: any[] = [];
        const departments: DepartmentLabel[] = [];
        let minLng = Infinity, minLat = Infinity, maxLng = -Infinity, maxLat = -Infinity;

        this.packs.forEach((pack) => {
            const deptCoords: [number, number][] = [];
            const packFeatures = (pack.geojson && pack.geojson.features) || [];
            packFeatures.forEach((f: any) => {
                features.push({
                    ...f,
                    properties: {
                        ...(f.properties || {}),
                        department_code: pack.code,
                        department_name: pack.name,
                    },
                });
                this.flattenCoords(f.geometry, deptCoords);
            });
            if (deptCoords.length) {
                const lngs = deptCoords.map((c) => c[0]);
                const lats = deptCoords.map((c) => c[1]);
                departments.push({
                    code: pack.code,
                    name: pack.name,
                    center: [
                        (Math.min(...lngs) + Math.max(...lngs)) / 2,
                        (Math.min(...lats) + Math.max(...lats)) / 2,
                    ],
                });
                deptCoords.forEach(([lng, lat]) => {
                    if (lng < minLng) minLng = lng;
                    if (lat < minLat) minLat = lat;
                    if (lng > maxLng) maxLng = lng;
                    if (lat > maxLat) maxLat = lat;
                });
            }
        });

        return {
            collection: { type: 'FeatureCollection', features },
            bounds: minLng === Infinity ? null : [[minLng, minLat], [maxLng, maxLat]],
            departments,
        };
    }

    private flattenCoords(geometry: any, out: [number, number][]) {
        if (!geometry) {
            return;
        }
        if (geometry.type === 'GeometryCollection') {
            (geometry.geometries || []).forEach((g: any) => this.flattenCoords(g, out));
            return;
        }
        const walk = (c: any) => {
            if (typeof c[0] === 'number') {
                out.push([c[0], c[1]]);
            } else {
                c.forEach(walk);
            }
        };
        if (geometry.coordinates) {
            walk(geometry.coordinates);
        }
    }
}
</script>

<style scoped lang="less">
.offline-packs-map {
    display: flex;
    flex-direction: column;
    gap: @vertical-margin-x-small;
}

.empty-map {
    color: @pale-sky;
    padding: 30px 4px;
    text-align: center;
    font-size: 0.9rem;
}

.opm-container {
    width: 100%;
    height: 55vh;
    min-height: 320px;
    border-radius: 4px;
    overflow: hidden;
    border: 1px solid #aaa;
}

.opm-note {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    color: @gunmetal;
    font-size: 0.8rem;

    i {
        color: @carrot-orange;
        flex-shrink: 0;
        margin-top: 2px;
    }
}
</style>
<style lang="less">
/* Étiquette de département (élément DOM custom MapLibre, hors du scope Vue). */
.department-label {
    background: white;
    border: 1px solid @pelorous;
    color: @pelorous;
    border-radius: 12px;
    padding: 2px 10px;
    font-size: 0.78rem;
    font-weight: 700;
    white-space: nowrap;
    box-shadow: 0 0 3px #0003;
    pointer-events: none;
}
</style>
