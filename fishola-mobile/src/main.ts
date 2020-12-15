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
import App from './App.vue'
import router from './router'
import VueLodash from 'vue-lodash'
import debounce from 'lodash/debounce'
import throttle from 'lodash/throttle'
import orderBy from 'lodash/orderBy'
import moment from 'moment'
import VModal from 'vue-js-modal'
import VueScrollTo from 'vue-scrollto';

moment.locale('fr');

Vue.use(VueLodash, { name: 'custom' , lodash: { debounce, throttle, orderBy, moment } })
Vue.use(VModal, { dialog: true });
Vue.use(VueScrollTo, {
     container: "#about-scroll-container",
     duration: 500,
     easing: "ease",
     offset: 0,
     force: true,
     cancelable: true,
     onStart: false,
     onDone: false,
     onCancel: false,
     x: false,
     y: true
 });
Vue.config.productionTip = false

new Vue({
  router,
  render: h => h(App)
}).$mount('#app');
