import Vue from 'vue'
import App from './App.vue'
import {initServiceProperties} from "./common/http-common";

Vue.config.productionTip = false

Vue.config.errorHandler = function(err, vm, info) {
  console.log(`GOT Vue ERROR`)
  vm.$store.commit('setError', 'Vue: ' + err + ' : ' + info)
}

window.onerror = function(message, source, lineno, colno, error) {
  console.log(`GOT window ERROR`)
  Vue.$store.commit('setError', message + ' ' + error)
}


initServiceProperties().then(p => {
    if (p === true) {
        new Vue({
            render: h => h(App)
        }).$mount('#app')
    } else {
       console.log('ServiceProperties could not be initialized');
    }
})

