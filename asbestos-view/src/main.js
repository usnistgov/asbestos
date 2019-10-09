import Vue from 'vue'
import App from './App.vue'

Vue.config.productionTip = false

Vue.config.errorHandler = function(err, vm, info) {
  console.log(`GOT Vue ERROR`)
  vm.$store.commit('setError', err + ' ' + info)
}

window.onerror = function(message, source, lineno, colno, error) {
  console.log(`GOT window ERROR`)
  Vue.$store.commit('setError', message + ' ' + error)
}

new Vue({
  render: h => h(App)
}).$mount('#app')
