import Vue from 'vue'
import VueRouter, { RouteConfig } from 'vue-router'

import Dispatcher from '@/views/Dispatcher.vue'
import Login from '@/views/Login.vue'
import Home from '@/views/Home.vue'

import Lakes from '@/views/referentials/Lakes.vue'
import Weathers from '@/views/referentials/Weathers.vue'
import Techniques from '@/views/referentials/Techniques.vue'
import Species from '@/views/referentials/Species.vue'

import SpeciesPerLake from '@/views/customize/SpeciesPerLake.vue'
import AuthorizedSamples from '@/views/customize/AuthorizedSamples.vue'

import Trips from '@/views/Trips.vue'

import Users from '@/views/Users.vue'

Vue.use(VueRouter)

const routes: Array<RouteConfig> = [
  {
    path: '/',
    name: 'dispatcher',
    component: Dispatcher
  },
  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/home',
    name: 'home',
    component: Home
  },
  {
    path: '/referentials/lakes',
    name: 'lakes',
    component: Lakes
  },
  {
    path: '/referentials/weathers',
    name: 'weathers',
    component: Weathers
  },
  {
    path: '/referentials/techniques',
    name: 'techniques',
    component: Techniques
  },
  {
    path: '/referentials/species',
    name: 'species',
    component: Species
  },
  {
    path: '/customize/species-per-lake',
    name: 'species-per-lake',
    component: SpeciesPerLake
  },
  {
    path: '/customize/authorized-samples',
    name: 'authorized-samples',
    component: AuthorizedSamples
  },
  {
    path: '/trips',
    name: 'trips',
    component: Trips
  },
  {
    path: '/users',
    name: 'users',
    component: Users
  }
]

const router = new VueRouter({
  routes
})

export default router
