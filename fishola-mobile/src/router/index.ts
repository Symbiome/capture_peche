import Vue from 'vue'
import VueRouter from 'vue-router'

import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import MyTrips from '../views/MyTrips.vue'
import NewTrip from '../views/NewTrip.vue'
import Dashboard from '../views/Dashboard.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
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
