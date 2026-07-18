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
  Recherche « par commune » (#6). Répond à l'exigence note Q1 (« recherche
  textuelle : nom du cours d'eau, commune »). L'utilisateur cherche une commune
  (autocomplete serveur, insensible casse/accents/typo), la sélectionne, puis
  choisit une entité hydro parmi celles de la commune (triées par distance au
  centroïde communal côté serveur). Le tap sur un item sélectionne l'entité
  (même effet que la carte / le mode liste).
  -->
<template>
<div class="commune-search">
    <i class="icon-error close-button" @click="close" />

    <div class="cs-header">
        <h3>Recherche par commune</h3>
        <div class="cs-input-wrapper">
            <input
                ref="communeInput"
                type="text"
                v-model="query"
                inputmode="search"
                autocomplete="off"
                placeholder="Nom de la commune…"
                v-on:input="onQueryInput"
            />
            <i
                v-if="query"
                class="icon-error cs-clear"
                @click="clearCommune"
                title="Effacer"
            />
            <ul v-if="suggestions.length > 0" class="cs-suggestions">
                <li
                    v-for="c in suggestions"
                    :key="c.insee"
                    @click="selectCommune(c)"
                >{{ c.name }}</li>
            </ul>
        </div>
    </div>

    <div class="cs-body">
        <div v-if="loading" class="cs-state">
            <div class="loader" />
            Recherche…
        </div>

        <div v-else-if="error" class="cs-state error">
            <i class="icon-warning" />
            <p>{{ error }}</p>
        </div>

        <div v-else-if="!selectedCommune" class="cs-state">
            <p>Saisissez une commune pour lister ses cours d'eau et plans d'eau.</p>
        </div>

        <div v-else-if="items.length === 0" class="cs-state">
            <p>Aucune entité hydro rattachée à {{ selectedCommune.name }}.</p>
        </div>

        <template v-else>
            <div class="cs-commune-label">
                <i class="icon-lake" />{{ selectedCommune.name }} — {{ items.length }} entité<span v-if="items.length > 1">s</span>
            </div>
            <ul class="cs-list">
                <li
                    v-for="item in items"
                    :key="item.waterEntityId"
                    @click="selectItem(item)"
                >
                    <span class="item-main">
                        <span class="item-name">{{ item.name }}</span>
                        <span class="item-kind">{{ kindLabel(item.kind) }}</span>
                    </span>
                    <span class="item-distance">{{ formatDistance(item.distanceM) }}</span>
                </li>
            </ul>
        </template>
    </div>
</div>
</template>

<script lang="ts">
import { NearbyWaterEntity, CommuneResult } from '@/pojos/BackendPojos';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import ReferentialService from '@/services/ReferentialService';

@Component
export default class CommuneSearch extends Vue {
    @Prop() isVisible: boolean;

    query = '';
    suggestions: CommuneResult[] = [];
    selectedCommune: CommuneResult | null = null;
    items: NearbyWaterEntity[] = [];
    loading = false;
    error = '';
    private searchSeq = 0;
    private fetchSeq = 0;
    private searchTimer: any = null;

    mounted() {
        if (this.isVisible) {
            this.focusInput();
        }
    }

    beforeDestroy() {
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
    }

    @Watch('isVisible')
    onVisibilityChange() {
        if (this.isVisible) {
            this.focusInput();
        }
    }

    private focusInput() {
        this.$nextTick(() => {
            const el = this.$refs.communeInput as HTMLInputElement;
            if (el) {
                el.focus();
            }
        });
    }

    // Recherche commune debouncée (250 ms, ≥ 2 caractères), avec garde
    // anti-réponse obsolète : seule la réponse de la dernière frappe s'applique.
    onQueryInput() {
        const term = this.query.trim();
        if (this.searchTimer) {
            clearTimeout(this.searchTimer);
        }
        if (term.length < 2) {
            this.suggestions = [];
            return;
        }
        this.searchTimer = setTimeout(() => {
            const seq = ++this.searchSeq;
            ReferentialService.searchCommunes(term)
                .then((results) => {
                    if (seq === this.searchSeq) {
                        this.suggestions = results || [];
                    }
                })
                .catch(() => { /* recherche en échec : on conserve l'état courant */ });
        }, 250);
    }

    selectCommune(commune: CommuneResult) {
        this.selectedCommune = commune;
        this.query = commune.name;
        this.suggestions = [];
        this.fetchEntities();
    }

    private fetchEntities() {
        if (!this.selectedCommune) {
            return;
        }
        this.loading = true;
        this.error = '';
        this.items = [];
        const seq = ++this.fetchSeq;
        ReferentialService.getWaterEntitiesByCommune(this.selectedCommune.insee)
            .then((results) => {
                if (seq !== this.fetchSeq) {
                    return;
                }
                this.items = results;
            })
            .catch(() => {
                if (seq === this.fetchSeq) {
                    this.error = "La recherche a échoué. Vérifiez votre connexion et réessayez.";
                }
            })
            .finally(() => {
                if (seq === this.fetchSeq) {
                    this.loading = false;
                }
            });
    }

    clearCommune() {
        this.query = '';
        this.suggestions = [];
        this.selectedCommune = null;
        this.items = [];
        this.focusInput();
    }

    selectItem(item: NearbyWaterEntity) {
        this.$emit('select', item);
    }

    kindLabel(kind: string): string {
        return kind === 'FLOWING' ? "Cours d'eau" : "Plan d'eau";
    }

    // « 820 m » sous le kilomètre, « 1,2 km » au-delà (séparateur décimal FR).
    formatDistance(m: number): string {
        if (m == null) {
            return '';
        }
        if (m < 1000) {
            return `${Math.round(m)} m`;
        }
        return `${(m / 1000).toFixed(1).replace('.', ',')} km`;
    }

    close() {
        this.$emit('close');
    }
}
</script>

<style scoped lang="less">
.commune-search {
    width: 100%;
    position: fixed;
    z-index: 1500;
    bottom: @footer-height;
    max-height: 70vh; // fallback si dvh non supporté
    max-height: calc(100dvh - @header-height - @secondary-header-height - @footer-height - 10px);
    left: 0;
    background-color: white;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    border-top-left-radius: 30px;
    border-top-right-radius: 30px;
    box-shadow: 0 -2px 10px #0003;

    @media screen and (min-width: @desktop-min-width) {
        position: absolute;
        top: 70px;
        left: 0;
        width: 100%;
        height: 500px;
        border: 1px solid #aaa;
        border-radius: 4px;
        box-shadow: 0px 2px 5px #0002;
    }
}

.close-button {
    position: absolute;
    top: 14px;
    right: 14px;
    z-index: 20;
    width: 30px;
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    color: @pale-sky;
    background: #f0f0f0;
    border-radius: 50%;
    cursor: pointer;
    transition: background 0.15s ease, color 0.15s ease;

    &:hover {
        background: @terra-cotta;
        color: white;
    }
}

.cs-header {
    padding: 20px 20px 12px;
    border-bottom: 1px solid #eee;

    h3 {
        margin: 0 0 10px;
        padding-right: 36px;
        font-size: 1.1rem;
        color: @gunmetal;
    }
}

.cs-input-wrapper {
    position: relative;

    input {
        width: 100%;
        height: 38px;
        padding: 0 32px 0 12px;
        border-radius: 4px;
        border: 1px solid @pale-sky;
        background: transparent;
        color: @gunmetal;
        font-size: @fontsize-form-input;
        font-family: 'Open Sans', sans-serif;
    }

    .cs-clear {
        position: absolute;
        top: 0;
        right: 8px;
        height: 38px;
        display: flex;
        align-items: center;
        cursor: pointer;
        &:hover { color: @terra-cotta; }
    }
}

.cs-suggestions {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    max-height: 220px;
    overflow-y: auto;
    margin: 2px 0 0;
    padding: 0;
    list-style: none;
    background: white;
    box-shadow: 0 0 5px #0002;
    z-index: 10;

    li {
        padding: 8px 12px;
        cursor: pointer;
        color: @gunmetal;
        &:hover { background: #0001; }
    }
}

.cs-body {
    flex: 1;
    overflow-y: auto;
    padding: 10px 0 20px;
}

.cs-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 15px;
    padding: 40px 30px;
    text-align: center;
    color: @pale-sky;

    &.error {
        color: @gunmetal;
        i { font-size: 2rem; color: @terra-cotta; }
    }
}

.cs-commune-label {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 20px 10px;
    color: @pale-sky;
    font-size: 0.85rem;
}

.cs-list {
    list-style: none;
    margin: 0;
    padding: 0;

    li {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 15px;
        padding: 12px 20px;
        border-bottom: 1px solid #f2f2f2;
        cursor: pointer;
        &:hover { background: #0001; }
    }

    .item-main {
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;
    }
    .item-name { color: @gunmetal; font-weight: 400; }
    .item-kind { font-size: 0.8rem; color: @pale-sky; }
    .item-distance {
        flex-shrink: 0;
        font-weight: 600;
        color: @pelorous;
        white-space: nowrap;
    }
}
</style>
