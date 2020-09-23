<template>
  <div>
    <div>
      <div class="control-panel-item-title">Test Session</div>
      <b-form-select class="control-panel-font" v-model="testSession" :options="testSessions"></b-form-select>
      <img id="add" src="../../assets/add-button.png" @click="add()"/>
      <img id="delete" src="../../assets/exclude-button-red.png" @click="del()"/>
    </div>
    <div>
      <div v-if="adding">
        <input v-model="newChannelName">
        <button @click="doAdd()">Add</button>
        <button @click="cancelAdd()">Cancel</button>
      </div>
      <div v-if="deleting">
        Are you sure you want to delete testSession {{$store.state.base.testSession}}?
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
                testSession: 'default',  // driven by drop down menu
                testSessions: [],  // drives drop down menu
              adding: false,
              deleting: false,
              deletingMessage: null,
              newChannelName: null,
            }
        },
        methods: {
          add() {
            this.adding = true;
          },
          doAdd() {
            this.$store.dispatch('newSession', this.newChannelName.trim());
            this.$store.commit('setSession', this.newChannelName.trim());
            this.newChannelName = null;
            this.adding = false;
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
            this.updateSessions();
            // if (this.testSessions.length !== 0)
            //   this.$store.commit('setSession', this.testSessions[0]);
            this.updateSession();
            this.adding = false;
            this.deleting = false;
          },
            updateSessions() {
                let options = []
                this.$store.state.base.sessions.forEach(function(ts) {
                    let it = { value: ts, text: ts }
                    options.push(it)
                })
                this.testSessions = options
            },
            updateSession() {
                this.testSession = this.$store.state.base.session
            },
            routeTo() {
                this.$router.push(`/session/${this.testSession}`)
            },
        },
        created() {
            if (this.$store.state.base.sessions.length === 0)
                this.$store.dispatch('loadSessions')
            // for startup
            this.updateSessions()
            this.updateSession()
        },
        mounted() {
            this.$store.subscribe((mutation) => {
                switch(mutation.type) {
                    case 'setSessions':  // to catch changes later
                        this.updateSessions()
                        this.updateSession()
                        break
                    case 'setSession':
                        this.updateSession()
                        break
                }
            })
        },
        watch: {
            'testSession': 'routeTo'
        },
        name: "SessionControlPanel"
    }
</script>

<style scoped>

</style>
