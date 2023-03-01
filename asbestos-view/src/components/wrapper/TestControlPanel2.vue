<template>
  <div>
    <div v-if="!selectable" class="not-available">Select FHIR Server</div>
    <div v-else>
      <div>
        <span class="control-panel-item-title" @click="openCollection()">Test Collections</span>
        &nbsp;<img id="reload" class="selectable" @click="reload(true)" src="../../assets/reload.png" title="Refresh Test Collections List"/>
        <br />
      </div>

      <div class="bold">
        Client:
      </div>
      <select :disabled="disabled" v-model="collection" v-bind:size="clientCollections.length" class="control-panel-list control-panel-font">
        <option v-for="(coll, colli) in clientCollections"
                v-bind:value="coll"
                :key="coll + colli"
                >
          {{coll}}
        </option>
      </select>
      <div class="bold">
        Server:
      </div>
      <select :disabled="disabled" v-model="collection" v-bind:size="serverCollections.length" class="control-panel-list control-panel-font">
        <option v-for="(coll, colli) in serverCollections"
                v-bind:value="coll"
                :key="coll + colli"
        >
          {{coll}}
        </option>
      </select>
    </div>
  </div>
</template>

<script>
import Vue from 'vue'
import { BFormSelect } from 'bootstrap-vue'
Vue.component('b-form-select', BFormSelect)
import errorHandlerMixin from '../../mixins/errorHandlerMixin'

export default {
  data() {
    return {
      collection: this.$router.currentRoute.params['testCollection'],
      testType: "Server", // Client or Server
    }
  },
  methods: {
    reload(refreshRoute) {
      this.$store.dispatch('loadTestCollectionNames').then(() => {
        if (refreshRoute === true) {
          // Also, check if URL is pointing to test collections
          // Add tooltip to the reset image: Reload Test Collection
          // Example https://fhirtoolkit.test:8082/session/default/channel/mhdtest_without_cslog/collection/Test_Documents
          // Update route if needed
          const currentRoutePath = this.$router.currentRoute.path
          const parts = currentRoutePath.split("/");
          if (! parts.includes('collection')) {
            const currentTcName = this.$store.state.testRunner.currentTestCollectionName
            console.debug('calling openTheCollection: ' + currentTcName)
            this.openTheCollection(currentTcName)
          }
        }
      })
    },
      collectionUpdated() {
       if (this.collection !== this.$store.state.testRunner.currentTestCollectionName) {
        this.collection = this.$store.state.testRunner.currentTestCollectionName
       }
      },
    localCollectionUpdated() {
      this.openCollection()
    },
    openTheCollection(collection) {
      console.debug('In openTheCollection.')
      if (!this.selectable)
        return;
      if (this.$store.state.testRunner.currentTestCollectionName !== collection) {
        this.$store.commit('setTestCollectionName', collection)
      }
      if (collection === undefined || collection === null)
        return;

      this.collection = collection
      const route = `/session/${this.session}/channel/${this.channelName}/collection/${collection}`
      const currentRoutePath = this.$router.currentRoute.path
      if (currentRoutePath !== route) {
          this.$router.push(route)
      } else {
        console.log('Route is already the same as current.')
      }
    },
    openCollection() {
      console.debug('In openCollection, from watch?' + this.collection)
      if (!this.selectable)
        return;
      this.$store.commit('setTestCollectionName', this.collection)
      if (this.collection === undefined || this.collection === null)
        return;
      this.openTheCollection(this.collection)
    },
    selectIndividual() {
      if (!this.selectable)
        return
      if (!this.$store.state.testRunner.currentTestCollectionName)
        return;
      if (!this.$store.state.testRunner.currentTest)
        return
      const route = `/session/${this.session}/channel/${this.channelName}/collection/${this.collection}/test/${this.testId}`
      this.$router.push(route)
    },
  },
  computed: {
    channelName() {
      return this.$store.state.base.channel.channelName;
    },
    collectionDisplaySize() {
      return this.clientCollections.length + this.serverCollections.length + 2
    },
    client() {
      return this.testType === 'Client'
    },
    collections: {
      get() {
        return (this.client)
            ? this.clientCollections
            : this.serverCollections
      }
    },
    clientCollections() {
      const igVersions = this.$store.state.base.channel.ccFhirIgName
      return this.$store.getters.clientTestCollectionNames(igVersions)
    },
    serverCollections() {
      const igVersions = this.$store.state.base.channel.ccFhirIgName
      return this.$store.getters.serverTestCollectionNames(igVersions)
    },
    session() {
      return this.$store.state.base.channel.testSession
    },
    channelId() {
      return this.$store.getters.getChannelId
    },
    selectable() {
      return (this.session !== undefined || this.session !== null) && (this.channelName !== undefined || this.channelName !== null);
    },
    testId: {
      set(name) {
        this.$store.commit('setCurrentTest', name)
      },
      get() {
        return this.$store.state.testRunner.currentTest
      }
    },
    testIds() {
      return this.$store.state.testRunner.testScriptNames
    },
  },
  created() {
    // this.collection
      console.debug('In TestControlPanel2 created.')
    this.reload()
  },
  mounted() {

  },
  props: [
    'disabled'
  ],
  watch: {
    'collection': 'openCollection',
  },
  mixins: [ errorHandlerMixin ],
  name: "TestControlPanel2"
}
</script>

<style scoped>
.active {
  background-color: lightgray;
}
.disabled {
  color: lightgray;
}
.not-available {
  color: red;
}
</style>
