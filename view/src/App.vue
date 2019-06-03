<template>
  <div id="app">
    <router-view></router-view>

  </div>
</template>

<script>
  import {store} from "./store/index"
import {router} from "./routes/router"

export default {
  name: 'app',
  router: router,
  store: store
}
</script>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
