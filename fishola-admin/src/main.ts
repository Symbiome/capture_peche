
document.documentElement.style
.setProperty('$primary', 'yellow');

import Vue from 'vue'
import Buefy from 'buefy'
import 'buefy/dist/buefy.css'



Vue.use(Buefy)

import App from './App.vue'
import router from './router'

Vue.config.productionTip = false



new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
