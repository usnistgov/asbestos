<template>
  <div>
    <div>
      <div class="control-panel-item-title">Test Session</div>

        <!--
        begin
        has problems
        CHANNEL/channels/all: Error: Request failed with status code 404
        -->
      <img id="add" src="../../assets/add-button.png" @click="add()"/>
      <img id="delete" src="../../assets/exclude-button-red.png" @click="del()"/>
      <!-- end -->
      <span v-if="details">
              <button type="button" @click="toggleDetails()">No Details</button>
      </span>
      <span v-else>
        <button type="button" @click="toggleDetails()">Details</button>
      </span>
    </div>
    <div v-if="details">
      {{ sessionConfigDetails }}
    </div>
    <select v-model="testSession" size="1" class="control-panel-font">
      <option v-for="(ts, tsi) in $store.state.base.sessionNames"
              v-bind:value="ts"
              :key="ts + tsi"
      >
        {{ ts }}
      </option>
    </select>
    <div>
      <div v-if="adding">
        <input v-model="newTsName">
        <button @click="doAdd()">Add</button>
        <button @click="cancelAdd()">Cancel</button>
      </div>
      <div v-if="deleting">
        <span>
          <!-- TODO set the div width to 100px or so -->
        This action will also delete all channel configurations for the test session. Are you sure you want to delete Test Session {{testSession}}?
        </span>
      </div>
      <div v-if="deleting">
        <button @click="confirmDel()">Delete</button>
        <button @click="cancelDel()">Cancel</button>
      </div>
    </div>
  </div>
</template>

<script>
import Vue from 'vue'
import { BFormSelect } from 'bootstrap-vue'
Vue.component('b-form-select', BFormSelect)
import {PROXY} from '../../common/http-common'
import { ButtonGroupPlugin, ButtonPlugin, ToastPlugin } from 'bootstrap-vue'
Vue.use(ButtonGroupPlugin)
Vue.use(ButtonPlugin)
Vue.use(ToastPlugin)



export default {
  data() {
    return {
      testSessions: [],  // drives drop down menu
      adding: false,
      deleting: false,
      deletingMessage: null,
      newTsName: null,
      details: false,
    }
  },
  props: [
  ],
  created() {
    //if (this.$store.state.base.sessions.length === 0)
    this.$store.dispatch('initSessionsStore')
    // for startup
    this.updateSessionsFromStore()
    this.updateCurrentSession()
  },
  mounted() {
    this.$store.subscribe((mutation) => {
      switch(mutation.type) {
        case 'setSessionNames':  // to catch changes later
          console.log('SCP(a) syncing up mutation.type: ' + mutation.type)
          this.updateSessionsFromStore()
          this.updateCurrentSession()
          break
        case 'setSession':
          console.log('SCP(b) syncing up mutation.type: ' + mutation.type)
          this.updateCurrentSession()
          break
      }
    })
  },
  watch: {
    'testSession': 'routeTo'
  },
  computed: {
    testSession: {
      set(id) {
        if (id !== this.$store.state.base.session) {
          console.log(`setting testSession ${id}`)
          this.$store.dispatch('selectSession', id);
        }
      },
      get() {
        const ts = this.$store.state.base.session
        // console.log('getting testSession ' + ts)
        return ts
      }
    },
    sessionConfigDetails() {
      const config = this.$store.getters.getSessionConfig;
      let returnString = ''
      if (!config) returnString += 'No includes.';
      if (config.includes.length === 0) {
        returnString += 'Includes no other sessions.';
      } else {
        returnString += `Includes sessions: ${config.includes}.`
      }
      if (config.sessionConfigLocked === true)
        returnString += ' Session configuration is locked.'
      return returnString
    }
  },
  methods: {
    changeTestSession(event) {
      this.testSession = event.target.options[event.target.options.selectedIndex].text;
      this.$store.dispatch('selectSession', this.testSession);
    },
    toggleDetails() {
      this.details = !this.details;
      const sessionConfig = this.$store.getters.getSessionConfig;
      if (this.details && (!sessionConfig || sessionConfig.name !== this.testSession)) {
        this.$store.dispatch('selectSession', this.testSession);
      }
    },
    add() {
      this.adding = true;
    },
    doAdd() {
      const name = this.newTsName.trim();
      if (name) {
          const that = this
        const url = `rw/testSession`
        console.log(`adding session ${url}`)
        PROXY.post(url, name) // /${newName}
                .then(response => {
                  that.$store.commit('setSessionNames', response.data)
                  that.$store.commit('setSession', name);
                  that.newTsName = null;
                  that.adding = false;
                  that.updateCurrentSession();
                })
                .catch(function (error) {
                  that.error(error)
                  that.newTsName = null
                  that.adding = false
                })
      }
    },
    cancelAdd() {
      this.newTsName = null;
      this.adding = false;
      this.deleting = false;
    },
    del() {
      this.deleting = true;

      //this.deleting = false;
    },
    cancelDel() {
      this.adding = false;
      this.deleting = false;
    },
    confirmDel() {
      const url = `rw/testSession/${this.testSession}`
      console.log(`${url}`)
        const that = this
      PROXY.delete(url)
              .then(response => {
                that.$store.commit('setSessionNames', response.data)
                if (response.data.length > 0) {
                  const obj = response.data[0];
                  console.log(`new session is ${obj}`)
                  that.$store.commit('setSession', obj);
                  this.updateSessionsFromStore();
                  this.updateCurrentSession();
                  this.adding = false;
                  this.deleting = false;
                }
              })
              .catch(function (error) {
                that.$store.commit('setError', url + ': ' + error)
                console.error(`${error} for ${url}`)
                  that.error('Delete failed')
              })
    },
    updateSessionsFromStore() {
      let options = []
      let i = 0;
      this.$store.state.base.sessionNames.forEach(function(ts) {
        options.push({ name: ts, id: i })
        i = i + 1;
      })
      this.testSessions = options
    },
    updateCurrentSession() {
      this.testSession = this.$store.state.base.session
    },
    routeTo() {
      this.$router.push(`/session/${this.testSession}`)
    },
    error(msg) {
      console.log(msg)
      this.$bvToast.toast(msg, {noCloseButton: true, title: 'Error'})
    },
  },
  name: "SessionControlPanel"
}
</script>

<style scoped>

</style>
