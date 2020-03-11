import Vue from 'vue'
import App from './App.vue'
import router from './router'
import VueLodash from 'vue-lodash'
import debounce from 'lodash/debounce'
import orderBy from 'lodash/orderBy'

Vue.use(VueLodash, { name: 'custom' , lodash: { debounce, orderBy } })
Vue.config.productionTip = false

new Vue({
  router,
  render: h => h(App)
}).$mount('#app');
