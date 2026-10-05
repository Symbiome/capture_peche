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
  Bandeau « hors-ligne » (#53, « dégradé mais pas caché ») : affiché en
  permanence tant que le réseau manque, avec le nombre de sorties en attente de
  synchronisation. Un tap mène à la liste des sorties, où la file d'attente est
  détaillée.
  -->
<template>
  <div
    v-if="offline"
    class="offline-banner"
    role="status"
    aria-live="polite"
    v-on:click="openTrips"
  >
    <i class="icon-error" />
    <span class="offline-banner-text">
      <strong>Hors-ligne</strong> · {{ pendingText() }}
    </span>
  </div>
</template>

<script lang="ts">
import NetworkStatusService from "@/services/NetworkStatusService";
import TripsService from "@/services/TripsService";
import { RouterUtils } from "@/router/RouterUtils";

import { Component, Vue, Watch } from "vue-property-decorator";

@Component
export default class OfflineBanner extends Vue {
  offline: boolean = NetworkStatusService.isOffline();
  pendingCount: number = 0;

  private unsubscribeNetwork?: () => void;

  created() {
    this.refreshPendingCount();
    this.unsubscribeNetwork = NetworkStatusService.subscribe((online) => {
      this.offline = !online;
      this.refreshPendingCount();
    });
  }

  beforeDestroy() {
    if (this.unsubscribeNetwork) {
      this.unsubscribeNetwork();
    }
  }

  /** Une sortie terminée hors-ligne aboutit toujours sur un changement de page. */
  @Watch("$route")
  onRouteChange() {
    this.refreshPendingCount();
  }

  refreshPendingCount() {
    TripsService.countPendingTrips().then(
      (count) => (this.pendingCount = count),
      (error) =>
        console.error("Comptage des sorties en attente impossible", error)
    );
  }

  pendingText(): string {
    if (this.pendingCount === 0) {
      return "vos saisies seront synchronisées au retour du réseau";
    }
    const plural = this.pendingCount > 1 ? "s" : "";
    return `${this.pendingCount} sortie${plural} en attente de synchronisation`;
  }

  openTrips() {
    RouterUtils.pushRouteNoDuplicate(this.$router, "/my-trips/list");
  }
}
</script>

<style scoped lang="less">
// Posé juste au-dessus du pied de page : visible sur toutes les pages sans
// masquer l'en-tête (titre, avatar, menu) ni le bouton d'action principal.
.offline-banner {
  position: fixed;
  left: 0;
  right: 0;
  bottom: @footer-height;
  z-index: 996;

  display: flex;
  align-items: center;
  justify-content: center;
  gap: @margin-x-small;

  padding: 6px @margin-medium;
  background-color: @carrot-orange;
  color: @white;
  font-size: @fontsize-small-paragraph;
  line-height: 1.3;
  text-align: center;
  cursor: pointer;

  i {
    flex-shrink: 0;
  }

  @media screen and (min-width: @desktop-min-width) {
    left: @desktop-menu-width;
  }
}
</style>
