<template>
  <div>
    <div v-if="!selectable" class="not-available">Select FHIR Server</div>
    <div v-else>
      <div>
        <span class="control-panel-item-title" @click="openCollection()">Test Collections</span>
        <img id="reload" class="selectable" @click="reload()" src="../../assets/reload.png"/>
        <br />
      </div>

      <div class="bold">
        Client:
      </div>
      <select v-model="collection" v-bind:size="clientCollections.length" class="control-panel-font">
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
      <select v-model="collection" v-bind:size="serverCollections.length" class="control-panel-font">
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
      collection: null,
      testType: "Server", // Client or Server
    }
  },
  methods: {
    reload() {
      this.$store.dispatch('loadTestCollectionNames')
    },
    vuexCollectionUpdated() {
      if (this.$store.state.testRunner.currentTestCollectionName === null)
        return;
      if (this.$store.state.testRunner.autoRoute && this.collection !== this.$store.state.testRunner.currentTestCollectionName) {
        this.collection = this.$store.state.testRunner.currentTestCollectionName
        this.openCollection()
      }
    },
    localCollectionUpdated() {
//                if (this.collection !== this.$store.state.testRunner.currentTestCollectionName)
      this.openCollection()
    },
    openTheCollection(collection) {
      if (!this.selectable)
        return;
      this.$store.commit('setTestCollectionName', collection)
      if (!collection)
        return;
      this.collection = collection
      const route = `/session/${this.session}/channel/${this.channelName}/collection/${collection}`
      this.$router.push(route)
    },
    openCollection() {
      if (!this.selectable)
        return;
      this.$store.commit('setTestCollectionName', this.collection)
      if (!this.collection)
        return;
      const route = `/session/${this.session}/channel/${this.channelName}/collection/${this.collection}`
      this.$router.push(route)
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
      return this.$store.state.base.channelName;
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
            ? this.$store.state.testRunner.clientTestCollectionNames
            : this.$store.state.testRunner.serverTestCollectionNames
      }
    },
    clientCollections() {
      return this.$store.state.testRunner.clientTestCollectionNames
    },
    serverCollections() {
      return this.$store.state.testRunner.serverTestCollectionNames
    },
    session() {
      return this.$store.state.base.channel.testSession
    },
    channelId() {
      // TODO: is this used anywhere?
      // May need to use the session from the channel config, not from the current [base.js] session
      return this.$store.getters.getChannelId
    },
    selectable() {
      return this.session && this.channelName;
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
    this.reload()
  },
  mounted() {

  },
  watch: {
    'channelName': 'reload',
    '$store.state.testRunner.currentTestCollectionName': 'vuexCollectionUpdated',
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
