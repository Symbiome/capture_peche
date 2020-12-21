/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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
import Vue from 'vue'
import VueRouter from 'vue-router'

import Dispatcher from '../views/Dispatcher.vue'

import About from '../views/About.vue'

import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import MyTrips from '../views/MyTrips.vue'
import Dashboard from '../views/Dashboard.vue'

import NewTrip from '../views/trip/NewTrip.vue'
import TripMeta from '../views/trip/TripMeta.vue'
import TripSpecies from '../views/trip/TripSpecies.vue'
import TripTechniques from '../views/trip/TripTechniques.vue'
import TripCatchs from '../views/trip/TripCatchs.vue'
import TripSummary from '../views/trip/TripSummary.vue'
import EditTrip from '../views/trip/EditTrip.vue'

import EditCatch from '../views/trip/EditCatch.vue'

import Faq from '../views/Faq.vue'
import Documentation from '../views/Documentation.vue'
import Credits from '../views/Credits.vue'

import Settings from '../views/Settings.vue'
import Profile from '../views/Profile.vue'
import ProfilePassword from '../views/ProfilePassword.vue'

import ResetPassword from '../views/ResetPassword.vue'
import VerifyAccount from '../views/VerifyAccount.vue'

import ProfileService from '@/services/ProfileService';

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'dispatcher',
    component: Dispatcher
  },
  {
    path: '/about',
    name: 'about',
    component: About
  },
  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/register',
    name: 'register',
    component: Register
  },
  {
    path: '/trips',
    name: 'trips',
    component: MyTrips
  },
  {
    path: '/trips/new',
    name: 'new-trip',
    component: NewTrip
  },
  {
    path: '/trips/:id/meta',
    name: 'trip-meta',
    component: TripMeta,
    props: true
  },
  {
    path: '/trips/:id/species',
    name: 'trip-species',
    component: TripSpecies,
    props: true
  },
  {
    path: '/trips/:id/catchs',
    name: 'trip-catchs',
    component: TripCatchs,
    props: true
  },
  {
    path: '/trips/:id/techniques',
    name: 'trip-techniques',
    component: TripTechniques,
    props: true
  },
  {
    path: '/trips/:tripId/catchs/:catchId',
    name: 'catch',
    component: EditCatch,
    props: true
  },
  {
    path: '/trips/:id/summary',
    name: 'trip-summary',
    component: TripSummary,
    props: true
  },
  {
    path: '/trips/:id',
    name: 'trip',
    component: EditTrip,
    props: true
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: Dashboard
  },
  {
    path: '/documentation',
    name: 'documentation',
    component: Documentation
  },
  {
    path: '/faq',
    name: 'faq',
    component: Faq
  },
  {
    path: '/settings',
    name: 'settings',
    component: Settings
  },
  {
    path: '/credits',
    name: 'credits',
    component: Credits
  },
  {
    path: '/profile',
    name: 'profile',
    component: Profile
  },
  {
    path: '/profile-password',
    name: 'profile-password',
    component: ProfilePassword
  },
  {
    path: '/reset-password/:token',
    name: 'reset-password',
    component: ResetPassword,
    props: true
  },
  {
    path: '/verify/:token',
    name: 'verify',
    component: VerifyAccount,
    props: true
  }
  // ,{
  //   path: '/about',
  //   name: 'about',
  //   // route level code-splitting
  //   // this generates a separate chunk (about.[hash].js) for this route
  //   // which is lazy-loaded when the route is visited.
  //   component: () => import(/* webpackChunkName: "about" */ '../views/About.vue')
  // }
]

const router = new VueRouter({
  routes
})

const publicRouteNames:string[] = [
  'dispatcher',
  'about',
  'faq',
  'documentation',
  'login',
  'register'
];

// Protect routes according to authentication status
router.beforeEach((to, from, next) => {
  if (to.name && publicRouteNames.indexOf(to.name) != -1) {
    next();
  } else {
    ProfileService.getProfile()
    .then(
      (profile) => {
        next();
      },
      (status) => {
        console.error("VOUS NE PASSEREZ PAS !", to.name);
        next('/');
      }
    );
  }
})

export default router
