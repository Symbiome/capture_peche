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
import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import moment from "moment";
import Vue3TouchEvents, { type Vue3TouchEventsOptions } from "vue3-touch-events";

import NoMenuLayout from "@/layouts/NoMenuLayout.vue";
import DefaultLayout from "@/layouts/DefaultLayout.vue";
import ThemeService from "@/services/ThemeService";

moment.locale("fr");

// Thème appliqué avant le premier rendu (#208) : pas de flash en clair.
ThemeService.init();

const app = createApp(App);
app.component("no-menu-layout", NoMenuLayout);
app.component("default-layout", DefaultLayout);
app.use(router);
app.use<Partial<Vue3TouchEventsOptions>>(Vue3TouchEvents, {});
app.mount("#app");
