import Vue from 'vue'
import App from './App.vue'
import vuetify from './plugins/vuetify';
import VueSocketIOExt from 'vue-socket.io-extended';
import { io } from 'socket.io-client';

import 'typeface-roboto/index.css'


Vue.config.productionTip = false

const socket = io(process.env.VUE_APP_BACKEND_URL);

Vue.use(VueSocketIOExt, socket);

new Vue({
  vuetify,
  render: h => h(App)
}).$mount('#app')
