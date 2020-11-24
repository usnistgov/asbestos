<template>
  <div>
    <div>
      <div class="control-panel-item-title">Test Session</div>
      <select v-model="testSession" size="1" class="control-panel-font">
        <option v-for="(ts, tsi) in $store.state.base.sessionNames"
                v-bind:value="ts"
                :key="ts + tsi"
        >
          {{ ts }}
        </option>
      </select>
        <!--
        begin
        has problems
        CHANNEL/channels/all: Error: Request failed with status code 404
        -->
<!--      <img id="add" src="../../assets/add-button.png" @click="add()"/>-->
<!--      <img id="delete" src="../../assets/exclude-button-red.png" @click="del()"/>-->
      <!-- end -->
      <span v-if="details">
              <button type="button" @click="toggleDetails()">No Details</button>
      </span>
      <span v-else>
        <button type="button" @click="toggleDetails()">Details</button>
      </span>
    </div>
    <div v-if="details">
      {{ includesSession }}
    </div>
    <div>
      <div v-if="adding">
        <input v-model="newChannelName">
        <button @click="doAdd()">Add</button>
        <button @click="cancelAdd()">Cancel</button>
      </div>
      <div v-if="deleting">
        Are you sure you want to delete testSession {{testSession}}?
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

export default {
  data() {
    return {
      testSessions: [],  // drives drop down menu
      adding: false,
      deleting: false,
      deletingMessage: null,
      newChannelName: null,
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
          this.updateSessionsFromStore()
          this.updateCurrentSession()
          break
        case 'setSession':
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
        return this.$store.state.base.session;
      }
    },
    includesSession() {
      const config = this.$store.getters.getSessionConfig;
      if (!config) return 'No includes';
      if (config.includes.length === 0) {
        return 'Includes no other sessions';
      } else {
        return `Includes sessions: ${config.includes}`
      }
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
      const name = this.newChannelName.trim();
      if (name) {
        this.$store.dispatch('newSession', name);
        this.$store.commit('setSession', name);
        this.newChannelName = null;
        this.adding = false;
        this.updateCurrentSession();
      }
    },
    cancelAdd() {
      this.newChannelName = null;
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
      this.$store.dispatch('delSession', this.testSession)
      this.updateSessionsFromStore();
      this.updateCurrentSession();
      this.adding = false;
      this.deleting = false;
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
  },
  name: "SessionControlPanel"
}
</script>

<style scoped>

</style>
