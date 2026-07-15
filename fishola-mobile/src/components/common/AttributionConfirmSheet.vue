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
  Bottom-sheet de confirmation d'attribution hydrographique (#9). À partir d'un
  point (pin/géoloc), affiche l'entité proposée par le backend
  (`/waterEntities/attribution`) et ses alternatives, avec l'avertissement « cours
  d'eau non permanent ». L'utilisateur confirme ou choisit une alternative.
  Réutilisé par la re-validation offline à la reconnexion (#10).
  -->
<template>
    <div v-if="visible" class="attribution-sheet-overlay" @click.self="cancel">
        <div class="attribution-sheet">
            <template v-if="proposal">
                <h3>Votre session sera rattachée à :</h3>

                <div
                    class="candidate proposal"
                    :class="{ selected: isSelected(proposal) }"
                    @click="select(proposal)"
                >
                    <span class="candidate-name">{{ proposal.name }}</span>
                    <span class="candidate-dist">{{ formatDistance(proposal.distanceM) }}</span>
                </div>

                <div v-if="showIntermittentWarning" class="warning">
                    <i class="icon-warning" />
                    Cours d'eau non permanent — vérifiez le rattachement.
                </div>

                <div v-if="alternatives.length" class="alternatives">
                    <p class="alternatives-label">Ou choisissez :</p>
                    <div
                        v-for="alt in alternatives"
                        :key="alt.waterEntityId"
                        class="candidate"
                        :class="{ selected: isSelected(alt) }"
                        @click="select(alt)"
                    >
                        <span class="candidate-name">{{ alt.name }}</span>
                        <span class="candidate-dist">{{ formatDistance(alt.distanceM) }}</span>
                    </div>
                </div>

                <div class="actions">
                    <button type="button" class="btn-cancel" @click="cancel">Annuler</button>
                    <button type="button" class="btn-confirm" :disabled="!selected" @click="confirm">
                        Confirmer
                    </button>
                </div>
            </template>

            <template v-else>
                <h3>Aucun plan/cours d'eau à proximité</h3>
                <p class="empty">Aucune entité hydrographique n'a été trouvée près de ce point.</p>
                <div class="actions">
                    <button type="button" class="btn-cancel" @click="cancel">Fermer</button>
                </div>
            </template>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import { AttributionResponse, WaterEntityAttribution } from '@/pojos/BackendPojos';

@Component
export default class AttributionConfirmSheet extends Vue {
    @Prop({ default: null }) attribution: AttributionResponse | null;
    @Prop({ default: false }) visible: boolean;

    selected: WaterEntityAttribution | null = null;

    get proposal(): WaterEntityAttribution | null {
        return this.attribution ? this.attribution.proposal ?? null : null;
    }

    get alternatives(): WaterEntityAttribution[] {
        return this.attribution ? this.attribution.alternatives ?? [] : [];
    }

    get showIntermittentWarning(): boolean {
        // persistent === false : tronçon non permanent (avertissement non bloquant).
        return !!this.selected && this.selected.persistent === false;
    }

    // À l'ouverture (ou au changement de proposition), pré-sélectionner la proposition.
    @Watch('attribution', { immediate: true })
    resetSelection() {
        this.selected = this.proposal;
    }

    isSelected(entity: WaterEntityAttribution): boolean {
        return !!this.selected && this.selected.waterEntityId === entity.waterEntityId;
    }

    select(entity: WaterEntityAttribution) {
        this.selected = entity;
    }

    formatDistance(distanceM: number): string {
        if (distanceM == null) {
            return '';
        }
        return distanceM < 1000
            ? `${Math.round(distanceM)} m`
            : `${(distanceM / 1000).toFixed(1)} km`;
    }

    confirm() {
        if (this.selected) {
            this.$emit('confirm', this.selected);
        }
    }

    cancel() {
        this.$emit('cancel');
    }
}
</script>

<style scoped lang="less">
.attribution-sheet-overlay {
    position: fixed;
    inset: 0;
    z-index: 2000;
    background: @black-alpha-90;
    display: flex;
    align-items: flex-end;
    justify-content: center;

    @media screen and (min-width: @desktop-min-width) {
        align-items: center;
    }
}

.attribution-sheet {
    width: 100%;
    max-width: 520px;
    max-height: 80vh;
    overflow-y: auto;
    background: white;
    border-top-left-radius: 20px;
    border-top-right-radius: 20px;
    padding: 20px;
    box-shadow: 0 -2px 12px #0003;

    @media screen and (min-width: @desktop-min-width) {
        border-radius: 12px;
    }

    h3 {
        margin: 0 0 15px;
        font-size: 1.1rem;
        color: @gunmetal;
    }
}

.candidate {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 15px;
    border: 1px solid @pale-sky;
    border-radius: 8px;
    margin-bottom: 8px;
    cursor: pointer;

    &.selected {
        border-color: @pelorous;
        background: #1e9bc422;
    }
    &.proposal .candidate-name {
        font-weight: bold;
    }
    .candidate-dist {
        color: @pale-sky;
        font-size: 0.9rem;
        white-space: nowrap;
        margin-left: 10px;
    }
}

.warning {
    display: flex;
    align-items: center;
    gap: 8px;
    color: @terra-cotta;
    font-size: 0.9rem;
    margin: 4px 0 12px;
}

.alternatives-label {
    margin: 12px 0 6px;
    color: @pale-sky;
    font-size: 0.9rem;
}

.empty {
    color: @pale-sky;
    margin-bottom: 15px;
}

.actions {
    display: flex;
    gap: 12px;
    margin-top: 16px;

    button {
        flex: 1;
        padding: 12px;
        border-radius: 8px;
        font-size: 1rem;
        cursor: pointer;
        border: none;
    }
    .btn-cancel {
        background: #eee;
        color: @gunmetal;
    }
    .btn-confirm {
        background: @pelorous;
        color: white;

        &:disabled {
            opacity: 0.5;
            cursor: default;
        }
        &:not(:disabled):hover {
            background: @terra-cotta;
        }
    }
}
</style>
