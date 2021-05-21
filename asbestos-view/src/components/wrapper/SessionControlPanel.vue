<template>
    <div>
        <div class="control-panel-item-title">Test Session</div>
        <div>
            <div class="tooltip">
                <img @click="add()" id="add" src="../../assets/add-button.png"/>
                <span class="tooltiptext">Add a New Test Session</span>
            </div>
            <div class="tooltip">
                <template v-if="! isSessionConfigLocked">
                    <img @click="del()" id="delete" src="../../assets/exclude-button-red.png"/>
                    <span class="tooltiptext">Delete Test Session</span>
                </template>
                <template v-else>
                    <img class="dimOpacity" disabled="disabled" src="../../assets/exclude-button-red.png"/>
                    <span class="tooltiptext">Configuration is locked</span>
                </template>
            </div>
            <!-- end -->
            <span v-if="details">
              <button @click="toggleDetails()" type="button">No Details</button>
            </span>
            <span v-else>
                <button @click="toggleDetails()" type="button">Details</button>
            </span>
            <div v-if="details">
                {{ sessionConfigDetails }}
            </div>
        </div>
        <select class="control-panel-list control-panel-font" size="1" v-model="testSession">
            <option :key="ts + tsi"
                    v-bind:value="ts"
                    v-for="(ts, tsi) in testSessionNames"
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
          This action will also delete all channel configurations for the test session. Are you sure you want to delete {{testSession}}?
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
    import {BFormSelect} from 'bootstrap-vue'

    Vue.component('b-form-select', BFormSelect)
    import {PROXY} from '../../common/http-common'
    import {ButtonGroupPlugin, ButtonPlugin, ToastPlugin} from 'bootstrap-vue'
    import channelMixin from "@/mixins/channelMixin";

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
        created() {
            //if (this.$store.state.base.sessions.length === 0)
            const sessionIdFromRoute = this.$router.currentRoute.params['sessionId']
            this.$store.dispatch('initSessionsStore', sessionIdFromRoute)
            // for startup
            // this.updateSessionsFromStore()
            // this.updateCurrentSession()
        },
        /*
      mounted() {
      this.$store.subscribe((mutation) => {
        switch(mutation.type) {
          case 'setSessionNames':  // to catch changes later
            console.log('SessionCP(a) syncing up mutation.type: ' + mutation.type)
            this.updateSessionsFromStore()
            this.updateCurrentSession()
            break
          case 'setSession':
            console.log('SessionCP(b) syncing up mutation.type: ' + mutation.type)
            this.updateCurrentSession()
            break
        }
      })
      },
      watch: {
      'testSession': 'routeTo'
      },
      */
        computed: {
            isSessionConfigLocked() {
                let sc = this.$store.getters.getSessionConfig
                if (sc !== undefined && sc !== null) {
                    return sc.sessionConfigLocked
                } else
                    return false
            },
            testSessionNames() {
                return this.$store.state.base.sessionNames
            },
            testSession: {
                set(id) {
                    if (id === undefined || id === null)
                        return
                    this.$store.dispatch('selectSession', id).then(() => {
                        // First attempt in finding a channel to load: does a local channel exist?
                        let chIds = this.$store.getters.getChannelIdsForSession
                        let channelIdToLoad = chIds[0]
                        if (chIds === undefined || chIds === null || channelIdToLoad === undefined) {
                            // Second attempt: does the default__default channel exist?
                            chIds = this.$store.getters.getChannelIdsForCurrentSession
                            if (chIds !== undefined && chIds !== null && chIds.length > 0) {
                                const theDefaultChannel = 'default__default'
                                if (chIds.includes(theDefaultChannel)) {
                                    channelIdToLoad = theDefaultChannel
                                } else {
                                    channelIdToLoad = chIds[0]
                                }
                            }
                        }

                        if (chIds !== undefined && chIds !== null && channelIdToLoad !== undefined) {
                            this.ftkLoadChannel(channelIdToLoad, true, true)
                        } else {
                            const errMsg = 'A channel is not available.'
                            console.log(errMsg)
                            this.$store.commit('setError', errMsg)
                            this.$store.commit('installChannel', null);
                        }

                    })
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
                    returnString += ' This session configuration is locked.'
                return returnString
            }
        },
        methods: {
            /*
            changeTestSession(event) {
              this.testSession = event.target.options[event.target.options.selectedIndex].text;
              this.$store.dispatch('selectSession', this.testSession);
            },
            */
            toggleDetails() {
                this.details = !this.details;
                /*
                const sessionConfig = this.$store.getters.getSessionConfig;
                if (this.details && (!sessionConfig || sessionConfig.name !== this.testSession)) {
                  this.$store.dispatch('selectSession', this.testSession);
                }
                 */
            },
            add() {
                this.adding = true;
            },
            doAdd() {
                const name = this.newTsName.trim();
                if (name) {
                    const url = `rw/testSession`
                    console.log(`adding session ${url}`)
                    PROXY.post(url, name) // /${newName}
                        .then(response => {
                            this.$store.commit('setSessionNames', response.data)
                            this.testSession = name
                            // this.$store.commit('setSession', name);
                            this.newTsName = null;
                            this.adding = false;
                        })
                        .catch(function (error) {
                            this.error(error)
                            this.newTsName = null
                            this.adding = false
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
                            // this.updateSessionsFromStore();
                            // this.updateCurrentSession();
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
            /*
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
             */
            error(msg) {
                console.log(msg)
                this.$bvToast.toast(msg, {noCloseButton: true, title: 'Error'})
            },
        },
        mixins: [channelMixin],
        name: "SessionControlPanel"
    }
</script>

<style scoped>
.dimOpacity {
    opacity: .25;
}
</style>
