import Vue from 'vue'
import VueRouter from 'vue-router'

import Dispatcher from '../views/Dispatcher.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import MyTrips from '../views/MyTrips.vue'
import Dashboard from '../views/Dashboard.vue'

import NewTrip from '../views/trip/NewTrip.vue'
import EditTripMeta from '../views/trip/EditTripMeta.vue'
import EditTripSpecies from '../views/trip/EditTripSpecies.vue'
import EditTrip from '../views/trip/EditTrip.vue'

Vue.use(VueRouter)

const routes = [
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
    name: 'edit-trip-meta',
    component: EditTripMeta,
    props: true
  },
  {
    path: '/trips/:id/species',
    name: 'edit-trip-species',
    component: EditTripSpecies,
    props: true
  },
  {
    path: '/trips/:id',
    name: 'edit-trip',
    component: EditTrip,
    props: true
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: Dashboard
  },
  {
    path: '/about',
    name: 'about',
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () => import(/* webpackChunkName: "about" */ '../views/About.vue')
  }
]

const router = new VueRouter({
  routes
})

export default router
