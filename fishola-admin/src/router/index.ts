/*-
 * #%L
 * Fishola :: Admin
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

import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

import Dispatcher from "@/views/Dispatcher.vue";
import Login from "@/views/Login.vue";
import Home from "@/views/Home.vue";
import Help from "@/views/Help.vue";

import Lakes from "@/views/referentials/Lakes.vue";
import Weathers from "@/views/referentials/Weathers.vue";
import Techniques from "@/views/referentials/Techniques.vue";
import Species from "@/views/referentials/Species.vue";
import Catches from "@/views/Catches.vue";
import CatchEditionPage from "@/views/CatchEditionPage.vue";
import EditorialPagesVue from "@/views/referentials/EditorialPages.vue";
import DocumentationVue from "@/views/referentials/Documentation.vue";
import NewsVue from "@/views/referentials/News.vue";
import Metrics from "@/views/Metrics.vue";

import SpeciesPerLake from "@/views/customize/SpeciesPerLake.vue";
import AuthorizedSamples from "@/views/customize/AuthorizedSamples.vue";
import Admins from "@/views/Admins.vue";
import Users from "@/views/Users.vue";

Vue.use(VueRouter);

const routes: Array<RouteConfig> = [
  {
    path: "/",
    name: "dispatcher",
    component: Dispatcher
  },
  {
    path: "/login",
    name: "login",
    component: Login
  },
  {
    path: "/home",
    name: "home",
    component: Home
  },
  {
    path: "/help",
    name: "help",
    component: Help
  },
  {
    path: "/referentials/lakes",
    name: "lakes",
    component: Lakes
  },
  {
    path: "/referentials/weathers",
    name: "weathers",
    component: Weathers
  },
  {
    path: "/referentials/techniques",
    name: "techniques",
    component: Techniques
  },
  {
    path: "/referentials/species",
    name: "species",
    component: Species
  },
  {
    path: "/customize/species-per-lake",
    name: "species-per-lake",
    component: SpeciesPerLake
  },
  {
    path: "/customize/authorized-samples",
    name: "authorized-samples",
    component: AuthorizedSamples
  },
  {
    path: "/trips",
    name: "trips",
    component: Catches
  },
  {
    path: "/catch/:catchId",
    name: "catch-edition",
    props: true,
    component: CatchEditionPage
  },
  {
    path: "/users",
    name: "users",
    component: Users
  },
  {
    path: "/admins",
    name: "admins",
    component: Admins
  },
  {
    path: "/editorial-pages",
    name: "editorial-pages",
    component: EditorialPagesVue
  },
  {
    path: "/documentation",
    name: "documentation",
    component: DocumentationVue
  },
  {
    path: "/news",
    name: "news",
    component: NewsVue
  },
  {
    path: "/metrics",
    name: "metrics",
    component: Metrics
  }
];

const router = new VueRouter({
  routes,
  mode: 'history',
  base: import.meta.env.VITE__BASE ?? '/',
  scrollBehavior (to) {
    if (to.hash) {
      return {
        el: to.hash,
      }
    }
  }
});

export default router;
