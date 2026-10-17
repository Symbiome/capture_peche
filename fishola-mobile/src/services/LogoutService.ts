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
import { Router } from "vue-router";

import Helpers from "@/services/Helpers";
import ProfileService from "@/services/ProfileService";
import TripsService from "@/services/TripsService";
import { RouterUtils } from "@/router/RouterUtils";
import EventBus from "@/services/EventBus";

/**
 * Déconnexion confirmée par l'utilisateur, avec un second avertissement si
 * une sortie est en cours (elle serait perdue).
 */
export default class LogoutService {
  static confirmAndLogout(router: Router): Promise<void> {
    return Helpers.confirm(
      "Voulez-vous vous déconnecter ?",
      "Déconnexion"
    ).then(
      () => LogoutService.logoutUnlessRunningTrip(router),
      () => undefined
    );
  }

  private static logoutUnlessRunningTrip(router: Router): Promise<void> {
    return TripsService.hasRunningTrip().then((hasRunningTrip: boolean) => {
      if (!hasRunningTrip) {
        return LogoutService.logout(router);
      }
      return Helpers.confirm(
          "Vous avez une sortie en cours, elle sera perdue. Êtes-vous sûr·e ?",
        "Déconnexion"
      ).then(
        () => LogoutService.logout(router),
        () => undefined
      );
    });
  }

  private static logout(router: Router): Promise<void> {
    return ProfileService.logout().then(() => {
      RouterUtils.pushRouteNoDuplicate(router, "/login");
      EventBus.emit("loggued-out");
    });
  }
}
