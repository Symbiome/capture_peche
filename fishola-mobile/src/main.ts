import Vue from 'vue'
import App from './App.vue'
import router from './router'
import VueLodash from 'vue-lodash'
import debounce from 'lodash/debounce'
import throttle from 'lodash/throttle'
import orderBy from 'lodash/orderBy'
import moment from 'moment'
import VModal from 'vue-js-modal'

moment.locale('fr');

Vue.use(VueLodash, { name: 'custom' , lodash: { debounce, throttle, orderBy, moment } })
Vue.use(VModal, { dialog: true });
Vue.config.productionTip = false

new Vue({
  router,
  render: h => h(App)
}).$mount('#app');
