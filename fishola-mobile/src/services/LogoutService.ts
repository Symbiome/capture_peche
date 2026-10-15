/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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
import Vue from "vue";

import Helpers from "@/services/Helpers";
import ProfileService from "@/services/ProfileService";
import TripsService from "@/services/TripsService";
import { RouterUtils } from "@/router/RouterUtils";

/**
 * Déconnexion confirmée par l'utilisateur, avec un second avertissement si
 * une sortie est en cours (elle serait perdue).
 */
export default class LogoutService {
  static confirmAndLogout(vm: Vue): Promise<void> {
    return Helpers.confirm(
      vm.$modal,
      "Voulez-vous vous déconnecter ?",
      "Déconnexion"
    ).then(
      () => LogoutService.logoutUnlessRunningTrip(vm),
      () => undefined
    );
  }

  private static logoutUnlessRunningTrip(vm: Vue): Promise<void> {
    return TripsService.hasRunningTrip().then((hasRunningTrip: boolean) => {
      if (!hasRunningTrip) {
        return LogoutService.logout(vm);
      }
      return Helpers.confirm(
        vm.$modal,
        "Vous avez une sortie en cours, elle sera perdue. Êtes-vous sûr·e ?",
        "Déconnexion"
      ).then(
        () => LogoutService.logout(vm),
        () => undefined
      );
    });
  }

  private static logout(vm: Vue): Promise<void> {
    return ProfileService.logout().then(() => {
      RouterUtils.pushRouteNoDuplicate(vm.$router, "/login");
      vm.$root.$emit("loggued-out");
    });
  }
}
