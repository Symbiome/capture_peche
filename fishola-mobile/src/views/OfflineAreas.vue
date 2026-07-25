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
  Écran « Zones hors-ligne » (#54). Permet de télécharger, façon Komoot, le
  réseau hydrographique d'un département pour le retrouver sur la carte sans
  connexion. Cadrage Symbiome : on télécharge UNIQUEMENT les entités hydro (pas
  de fond de carte / tuiles). Le fond IGN reste donc indisponible hors-ligne
  (mode dégradé assumé) ; seules les entités permettent la sélection.
-->
<template>
  <div class="offline-areas page-with-header-and-footer shifted-background">
    <FisholaHeader />
    <div class="offline-areas-page page">
      <div class="pane">
        <div class="pane-content rounded">
          <h1>Zones hors-ligne</h1>

          <p class="intro">
            Téléchargez le réseau hydrographique d'un département pour le
            retrouver sur la carte <strong>sans connexion</strong>. Seuls les
            <strong>cours d'eau et plans d'eau</strong> sont enregistrés — le
            fond de carte (plan / satellite) n'est pas téléchargé et
            n'apparaîtra donc pas hors-ligne.
          </p>

          <div class="offline-banner" v-if="offline">
            <i class="icon-error" />
            <span>
              Vous êtes hors-ligne : la liste des départements et le
              téléchargement ne sont pas disponibles. Vos zones déjà
              téléchargées restent utilisables.
            </span>
          </div>

          <!-- Packs installés (disponibles même hors-ligne). -->
          <section v-if="packs.length" class="block">
            <h2>Départements téléchargés</h2>
            <ul class="pack-list">
              <li v-for="pack in packs" :key="pack.code" class="pack">
                <div class="pack-main">
                  <span class="pack-name">
                    <span class="dept-code">{{ pack.code }}</span>
                    {{ pack.name }}
                  </span>
                  <span class="pack-meta">
                    {{ pack.entityCount }} entité{{ pack.entityCount > 1 ? 's' : '' }}
                    · {{ formatBytes(pack.bytes) }}
                    · {{ formatDate(pack.downloadedAt) }}
                  </span>
                </div>
                <div class="pack-actions">
                  <button
                    type="button"
                    class="link-btn"
                    :disabled="offline || downloadingCode === pack.code"
                    @click="download(pack.code, pack.name)"
                    title="Mettre à jour"
                  >
                    {{ downloadingCode === pack.code ? '…' : 'Mettre à jour' }}
                  </button>
                  <button
                    type="button"
                    class="link-btn danger"
                    @click="remove(pack)"
                    title="Supprimer"
                  >
                    Supprimer
                  </button>
                </div>
              </li>
            </ul>
          </section>

          <!-- Ajout d'un département (nécessite une connexion). -->
          <section class="block" v-if="!offline">
            <h2>Ajouter un département</h2>

            <span class="input-wrapper">
              <input
                type="text"
                v-model="filter"
                inputmode="search"
                autocomplete="off"
                placeholder="Filtrer par nom ou code (ex. « 74 », « Savoie »)"
              />
            </span>

            <div v-if="loadingDepartments" class="loading-row">
              <div class="mini-spinner" /> Chargement des départements…
            </div>

            <ul v-else class="dept-list">
              <li
                v-for="dep in filteredDepartments"
                :key="dep.code"
                class="dept"
              >
                <span class="dept-label">
                  <span class="dept-code">{{ dep.code }}</span>
                  {{ dep.name }}
                  <small class="dept-communes">{{ dep.communeCount }} communes</small>
                </span>
                <button
                  v-if="downloadedCodes.has(dep.code)"
                  type="button"
                  class="pill installed"
                  disabled
                >
                  <i class="icon-check" /> Téléchargé
                </button>
                <button
                  v-else
                  type="button"
                  class="pill"
                  :disabled="downloadingCode === dep.code"
                  @click="download(dep.code, dep.name)"
                >
                  {{ downloadingCode === dep.code ? 'Téléchargement…' : 'Télécharger' }}
                </button>
              </li>
              <li v-if="!filteredDepartments.length" class="empty">
                Aucun département ne correspond.
              </li>
            </ul>
          </section>

          <div class="bottom-page-spacer"></div>
        </div>
      </div>
    </div>
    <FisholaFooter shortcuts="back,settings,profile" selected="settings" />
  </div>
</template>

<script lang="ts">
import FisholaHeader from "@/components/layout/FisholaHeader.vue";
import FisholaFooter from "@/components/layout/FisholaFooter.vue";

import OfflineAreasService, {
  DepartmentSummary,
} from "@/services/OfflineAreasService";
import OfflineArea from "@/pojos/OfflineArea";
import NetworkStatusService from "@/services/NetworkStatusService";
import Helpers from "@/services/Helpers";

import { Component, Vue } from "vue-property-decorator";

@Component({
  components: {
    FisholaHeader,
    FisholaFooter,
  },
})
export default class OfflineAreasView extends Vue {
  departments: DepartmentSummary[] = [];
  packs: OfflineArea[] = [];
  downloadedCodes: Set<string> = new Set();
  filter: string = "";
  loadingDepartments: boolean = true;
  downloadingCode: string | null = null;
  offline: boolean = false;
  private unsubscribeNetwork: (() => void) | null = null;

  mounted() {
    this.offline = NetworkStatusService.isOffline();
    // Les packs installés sont lisibles hors-ligne ; la liste distante non.
    this.loadPacks();
    this.loadDepartments();
    this.unsubscribeNetwork = NetworkStatusService.subscribe((online) => {
      this.offline = !online;
      if (online && !this.departments.length) {
        this.loadDepartments();
      }
    });
  }

  beforeDestroy() {
    if (this.unsubscribeNetwork) {
      this.unsubscribeNetwork();
    }
  }

  get filteredDepartments(): DepartmentSummary[] {
    const term = this.filter.trim().toLowerCase();
    if (!term) {
      return this.departments;
    }
    return this.departments.filter(
      (d) =>
        d.code.toLowerCase().startsWith(term) ||
        d.name.toLowerCase().indexOf(term) >= 0
    );
  }

  async loadPacks() {
    this.packs = await OfflineAreasService.listPacks();
    this.downloadedCodes = new Set(this.packs.map((p) => p.code));
  }

  loadDepartments() {
    if (NetworkStatusService.isOffline()) {
      this.loadingDepartments = false;
      return;
    }
    this.loadingDepartments = true;
    OfflineAreasService.listDepartments()
      .then((deps) => {
        this.departments = deps;
        this.loadingDepartments = false;
      })
      .catch(() => {
        this.loadingDepartments = false;
        this.offline = NetworkStatusService.isOffline();
      });
  }

  async download(code: string, name: string) {
    if (this.downloadingCode) {
      return;
    }
    this.downloadingCode = code;
    try {
      const area = await OfflineAreasService.downloadPack(code, name);
      await this.loadPacks();
      this.$root.$emit(
        "toaster-success",
        `${name} téléchargé (${area.entityCount} entités)`
      );
    } catch (e) {
      this.$root.$emit(
        "toaster-error",
        "Échec du téléchargement. Vérifiez votre connexion et réessayez."
      );
    } finally {
      this.downloadingCode = null;
    }
  }

  async remove(pack: OfflineArea) {
    try {
      await Helpers.confirm(
        this.$modal,
        `Supprimer le pack hors-ligne « ${pack.name} » ? Vous pourrez le retélécharger plus tard.`,
        "Supprimer la zone",
        "Annuler",
        "Supprimer"
      );
    } catch {
      return; // annulé
    }
    await OfflineAreasService.deletePack(pack.code);
    await this.loadPacks();
    this.$root.$emit("toaster-success", `${pack.name} supprimé`);
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} o`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(0)} Ko`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  formatDate(ts: number): string {
    try {
      return new Date(ts).toLocaleDateString("fr-FR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch {
      return "";
    }
  }
}
</script>

<style scoped lang="less">
.offline-areas-page {
  .intro {
    color: @pale-sky;
    font-size: 0.9rem;
    line-height: 1.4;
    margin-bottom: @vertical-margin-small;
  }

  .offline-banner {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    background: #fff4e5;
    border: 1px solid @carrot-orange;
    border-radius: 6px;
    padding: 10px 12px;
    margin-bottom: @vertical-margin-small;
    color: @gunmetal;
    font-size: 0.85rem;

    i {
      color: @carrot-orange;
      flex-shrink: 0;
      margin-top: 2px;
    }
  }

  .block {
    margin-top: @vertical-margin-medium;

    h2 {
      font-size: 1rem;
      font-weight: 600;
      color: @gunmetal;
      margin-bottom: @vertical-margin-xx-small;
    }
  }

  .input-wrapper {
    display: block;
    margin-bottom: @vertical-margin-x-small;

    input {
      width: 100%;
      height: 38px;
      padding: 0 12px;
      border-radius: 4px;
      border: 1px solid @pale-sky;
      background: transparent;
      color: @gunmetal;
      font-size: @fontsize-form-input;
      font-family: "Open Sans", sans-serif;
    }
  }

  .loading-row {
    display: flex;
    align-items: center;
    gap: 10px;
    color: @pale-sky;
    padding: 10px 0;
  }

  .mini-spinner {
    height: 18px;
    width: 18px;
    border-radius: 50%;
    border-top: 2px solid @pelorous;
    border-left: 2px solid @pelorous;
    border-right: 2px solid transparent;
    border-bottom: 2px solid transparent;
    animation: spin 1s linear infinite;
  }

  .dept-code {
    display: inline-block;
    min-width: 30px;
    padding: 1px 6px;
    margin-right: 6px;
    border-radius: 4px;
    background: #1e9bc422;
    color: @pelorous;
    font-weight: 700;
    font-size: 0.8rem;
    text-align: center;
  }

  .pack-list,
  .dept-list {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  .pack,
  .dept {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 10px 4px;
    border-bottom: 1px solid #0001;
  }

  .pack-main {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .pack-name,
  .dept-label {
    color: @gunmetal;
    font-size: 0.95rem;
  }

  .pack-meta,
  .dept-communes {
    color: @pale-sky;
    font-size: 0.78rem;
  }

  .dept-communes {
    margin-left: 6px;
  }

  .pack-actions {
    display: flex;
    gap: 12px;
    flex-shrink: 0;
  }

  .link-btn {
    background: none;
    border: none;
    color: @pelorous;
    cursor: pointer;
    font-size: 0.85rem;
    padding: 4px;

    &.danger {
      color: @cardinal;
    }
    &:disabled {
      color: @pale-sky;
      cursor: default;
    }
  }

  .pill {
    flex-shrink: 0;
    background-color: @pelorous;
    color: white;
    border: none;
    border-radius: 20px;
    padding: 7px 14px;
    font-size: 0.82rem;
    cursor: pointer;

    &:hover:not(:disabled) {
      background-color: @terra-cotta;
    }
    &:disabled {
      cursor: default;
      opacity: 0.8;
    }
    &.installed {
      background: transparent;
      color: @pelorous;
      border: 1px solid @pelorous;
    }
  }

  .empty {
    color: @pale-sky;
    padding: 12px 4px;
    font-size: 0.88rem;
  }
}
</style>
