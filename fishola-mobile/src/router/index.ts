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
import Vue from "vue";
import VueRouter from "vue-router";

import Dispatcher from "@/views/Dispatcher.vue";

import About from "@/views/About.vue";
import OfflineHome from "@/views/OfflineHome.vue";

import Login from "@/views/Login.vue";
import Register from "@/views/Register.vue";
import TripsAndNews from "@/views/TripsAndNews.vue";
import Dashboard from "@/views/Dashboard.vue";

import FishingLicences from "@/views/FishingLicences.vue";
import NewFishingLicence from "@/components/fishing-licences/NewFishingLicence.vue";
import EditFishingLicence from "@/components/fishing-licences/EditFishingLicence.vue";
import FishingLicenceFullScreen from "@/components/fishing-licences/FishingLicenceFullScreen.vue";

import NewTrip from "@/views/trip/NewTrip.vue";
import TripMeta from "@/views/trip/TripMeta.vue";
import TripSpecies from "@/views/trip/TripSpecies.vue";
import TripTechniques from "@/views/trip/TripTechniques.vue";
import TripCatchs from "@/views/trip/TripCatchs.vue";
import TripSummary from "@/views/trip/TripSummary.vue";
import EditTrip from "@/views/trip/EditTrip.vue";

import EditCatch from "@/views/trip/EditCatch.vue";

import Documentation from "@/views/Documentation.vue";
import NewsDetailsVue from "@/views/NewsDetails.vue";
import Credits from "@/views/Credits.vue";

import GaleryFull from "@/components/galery/GaleryFull.vue";

import OpenCVSizeComputation from "@/components/opencv/OpenCVSizeComputation.vue";

import Settings from "@/views/Settings.vue";
import Profile from "@/views/Profile.vue";
import ProfilePassword from "@/views/ProfilePassword.vue";

import ResetPassword from "@/views/ResetPassword.vue";
import VerifyAccount from "@/views/VerifyAccount.vue";

import ProfileService from "@/services/ProfileService";

Vue.use(VueRouter);

const routes = [
  {
    path: "/",
    name: "dispatcher",
    meta: {
      layout: "no-menu",
      public: true,
    },
    component: Dispatcher,
  },
  {
    path: "/about",
    name: "about",
    meta: {
      layout: "no-menu",
      public: true,
    },
    component: About,
  },
  {
    path: "/offline-home/:defaultTab",
    name: "offline-home",
    component: OfflineHome,
    props: true,
    meta: {
      public: true,
    },
  },
  {
    path: "/login",
    name: "login",
    meta: { public: true },
    component: Login,
  },
  {
    path: "/register",
    name: "register",
    meta: { public: true },
    component: Register,
  },
  {
    path: "/trips",
    name: "trips",
    component: TripsAndNews,
  },
  {
    path: "/trips/new",
    name: "new-trip",
    component: NewTrip,
  },
  {
    path: "/trips/:id/meta",
    name: "trip-meta",
    component: TripMeta,
    props: true,
  },
  {
    path: "/trips/:id/species",
    name: "trip-species",
    component: TripSpecies,
    props: true,
  },
  {
    path: "/trips/:id/catchs",
    name: "trip-catchs",
    component: TripCatchs,
    props: true,
  },
  {
    path: "/trips/:id/techniques",
    name: "trip-techniques",
    component: TripTechniques,
    props: true,
  },
  {
    path: "/trips/:tripId/catchs/:catchId",
    name: "catch",
    component: EditCatch,
    props: true,
  },
  {
    path: "/trips/:id/summary",
    name: "trip-summary",
    component: TripSummary,
    props: true,
  },
  {
    path: "/trips/:id",
    name: "trip",
    component: EditTrip,
    props: true,
  },
  {
    path: "/dashboard",
    name: "dashboard",
    component: Dashboard,
  },
  {
    path: "/documentation",
    name: "documentation",
    meta: { public: true },
    component: Documentation,
  },
  {
    path: "/news/:newsId",
    name: "news-details",
    meta: { public: true },
    component: NewsDetailsVue,
    props: true,
  },
  {
    path: "/licences",
    name: "licences",
    component: FishingLicences,
  },
  {
    path: "/licences/new",
    name: "licence-new",
    component: NewFishingLicence,
  },
  {
    path: "/licences-edit/:id",
    name: "licence-edit",
    component: EditFishingLicence,
    props: true,
  },
  {
    path: "/licences-fullscreen/:type/:id",
    name: "licence-fullscreen",
    component: FishingLicenceFullScreen,
    props: true,
  },
  {
    path: "/documentation/:tab",
    name: "documentationFaq",
    meta: { public: true },
    component: Documentation,
    props: true,
  },
  {
    path: "/settings",
    name: "settings",
    component: Settings,
  },
  {
    path: "/credits",
    name: "credits",
    meta: { public: true },
    component: Credits,
  },
  {
    path: "/profile",
    name: "profile",
    component: Profile,
  },
  {
    path: "/profile-password",
    name: "profile-password",
    component: ProfilePassword,
  },
  {
    path: "/reset-password/:token",
    name: "reset-password",
    meta: {
      layout: "no-menu",
      public: true,
    },
    component: ResetPassword,
    props: true,
  },
  {
    path: "/verify/:token",
    name: "verify",
    meta: {
      public: true,
    },
    component: VerifyAccount,
  },
  {
    path: "/galery",
    name: "galery",
    component: GaleryFull,
    props: true,
  },
  {
    path: "/fish-measure-test",
    name: "fish-measure-test",
    meta: {
      public: true,
    },
    component: OpenCVSizeComputation,
  },
  // ,{
  //   path: '/about',
  //   name: 'about',
  //   // route level code-splitting
  //   // this generates a separate chunk (about.[hash].js) for this route
  //   // which is lazy-loaded when the route is visited.
  //   component: () => import(/* webpackChunkName: "about" */ '@/views/About.vue')
  // }
];

const router = new VueRouter({
  routes,
});

// Protect routes according to authentication status
router.beforeEach((to, _from, next) => {
  if (to.meta && to.meta.public) {
    next();
  } else {
    ProfileService.getProfile().then(
      (_profile) => {
        next();
      },
      (_status) => {
        console.error("Route non autorisée : ", to.name);
        next("/login");
      }
    );
  }
});

export default router;
