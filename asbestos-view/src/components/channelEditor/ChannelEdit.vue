<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
  <div>
    <!-- begin grid -->
    <div class="window">
      <div v-if="channel" class="grid-container">
        <div class="button-bar">
          <div v-if="isEditMode">
            <div v-if="badNameMode">
              Cannot save with this name - {{ badNameModeReason }}
              <button class="cancel-button" @click="badNameCanceled">Continue</button>
            </div>
            <div v-else>
              <div class="tooltip">
                <img id="save-button" src="../../assets/save.png" @click="save"/>
                <span class="tooltiptext">Save</span>
              </div>
              <div class="divider"></div>
              <div class="divider"></div>
              <div class="divider"></div>
              <div class="tooltip">
                <img id="cancel-edit-button" src="../../assets/cancel.png" @click="discard()"/>
                <span class="tooltiptext">Discard</span>
              </div>
            </div>
          </div>
          <div v-else>
            <div v-if="ackMode">
              Delete?
              <button class="ok-button" @click="deleteAcked()">Ok</button>
              <button class="cancel-button" @click="deleteCanceled">Cancel</button>
            </div>
            <div v-else-if="lockAckMode">
              <sign-in :banner="lockAckMode" :userProps="editUserProps" :doDefaultSignIn="true" :showCancelButton="true" @onOkClick="lockAcked" @onCancelClick="lockCanceled" />
            </div>
            <div v-else>
              <div class="divider"></div>
              <div class="tooltip">
                <img id="edit-button" src="../../assets/pencil-edit-button.png" @click="guardedFn('Edit',toggleEdit)"/>
                <span class="tooltiptext">Edit</span>
              </div>
              <div class="divider"></div>
              <div class="tooltip">
                <img id="copy-button" src="../../assets/copy-document.png" @click="copy()"/>
                <span class="tooltiptext">Copy</span>
              </div>
              <div class="divider"></div>
              <div class="divider"></div>
              <div class="divider"></div>
              <div v-if="channel.writeLocked" class="tooltip">
                <img id="unlock-button" src="../../assets/lock-icon.png" @click="requestLock(false)"/>
                <span class="tooltiptext">Configuration is locked.</span>
              </div>
              <div v-else class="tooltip">
                <img id="lock-button" src="../../assets/unlock-icon.png" @click="requestLock(true)"/>
                <span class="tooltiptext">Configuration is unlocked.</span>
              </div>
            </div>

          </div>

        </div>

        <template v-if="editUserProps.signedIn">
          <div v-if="channelIsNew" class="grid-item">
            <label class="grid-name">Locked?</label>
            <input type="checkbox" v-model="channel.writeLocked">
          </div>
        </template>

        <label class="grid-name">Id</label>
        <div v-if="channelIsNew" class="grid-item">
          <input v-model="channel.channelName">
        </div>
        <div v-else class="grid-item">{{ channel.channelName }}</div>

        <label class="grid-name">Test Session</label>
        <div class="grid-item">{{ channel.testSession }}</div>

        <label class="grid-name">Environment</label>
        <div v-if="isEditMode" class="grid-item">
          <select v-model="channel.environment">
            <option v-for="e in $store.state.base.environments" :key="e">
              {{e}}
            </option>
          </select>
        </div>
        <div v-else class="grid-item">{{ channel.environment }}</div>

        <label class="grid-name">Channel Type</label>
        <div v-if="isEditMode" class="grid-item">
          <select v-model="channel.channelType">
            <option v-for="ct in $store.state.channel.channelTypes" :key="ct">
              {{ct}}
            </option>
          </select>
        </div>
        <div v-else class="grid-item">{{ channel.channelType }}</div>

        <label class="grid-name">Fhir Base</label>
        <div v-if="isEditMode" class="grid-item">
          <input v-model="channel.fhirBase">
          Only used with Channel Type fhir
        </div>
        <div v-else class="grid-item">{{ channel.fhirBase }}</div>

        <label class="grid-name">XDS Site Name</label>
        <div v-if="isEditMode" class="grid-item">
          <input v-model="channel.xdsSiteName">
          Only used with Channel Type mhd
        </div>
        <div v-else class="grid-item">{{ channel.xdsSiteName }}</div>

        <label v-if="channel.channelType === 'mhd'" class="grid-name">Log MHD Capability Statement Request?</label>
        <div v-if="channel.channelType === 'mhd' && isEditMode" class="grid-item">
          <input type="radio" id="noLogCapStmt" value="false" v-model="channel.logMhdCapabilityStatementRequest">
          <label for="noLogCapStmt">No</label>
          <input type="radio" id="logCapStmt" value="true" v-model="channel.logMhdCapabilityStatementRequest">
          <label for="logCapStmt">Yes</label>
        </div>
        <div v-else-if="channel.channelType === 'mhd'" class="grid-item">
          <div v-if="channel.logMhdCapabilityStatementRequest">
            Yes
          </div>
          <div v-else>
            No
          </div>
        </div>

        <label class="grid-name">MHD Version Support Option</label>
        <div v-if="isEditMode" class="grid-item" >
          <select size="1"  v-model="channelMhdVersionSupport">
            <option :key="eKey"
                    :value="e"
                    v-for="(e,eKey) in $store.state.channel.mhdVersions">
              {{ e }}
            </option>
          </select>
          Only used with Channel Type mhd
<!--            No selection is required if PDB Profile Canonical URI is unique and differentiable. I.e., if no Options are selected, channel validation is based on the PDB bundle profile. All test collections are displayed if no Options are selected. If Option(s) are selected, test collections are filtered based on Option selection.-->
        </div>
        <div v-else>
            <template v-if="Array.isArray(channel.mhdVersions) && channel.mhdVersions.length > 0">
              {{channel.mhdVersions.join(", ")}}
            </template>
          <template v-else>
            Use default (MHDv3.x)
          </template>
        </div>

        <div v-if="!lockAckMode && !isEditMode && !channel.fhirBase && !channel.xdsSiteName" class="channelError">
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          Warning: FhirBase or XDS Site Name must be present
        </div>
        <div v-if="!lockAckMode && !isEditMode && channel.channelType === 'fhir' && !channel.fhirBase" class="channelError">
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          Warning: FHIR type is selected but no FHIR Base is configured
        </div>
        <div v-if="!lockAckMode && !isEditMode && channel.channelType === 'mhd' && !channel.xdsSiteName" class="channelError">
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          Warning: MHD type is selected but no XDS Site Name is configured
        </div>
      </div>
    </div>
    <!-- end of grid -->
    <div v-if="channel && !isEditMode">
      <div>
        <p class="caption">HTTP Channel Base Address: </p>
        <span class="center">{{getChannelBase(false, channel)}}</span>
      </div>
      <div v-if="isHttpsMode">
        <p class="caption">HTTPS Channel Base Address: </p>
        <span class="center">{{getChannelBase(true, channel)}}</span>
      </div>
      <div>
        <p>Send to a channel base address above and</p>
        <ul>
          <li>Proxy will record your transaction</li>
          <li>Proxy will forward your transaction to
            <span v-if="channel.fhirBase">{{channel.fhirBase}}</span>
            <span v-if="channel.xdsSiteName">XDS Toolkit site {{channel.xdsSiteName}}</span>
          </li>
        </ul>
      </div>
    </div>
    <div v-if="channel===undefined || channel===null">
       Channel is undefined or null.
    </div>
  </div>
</template>

<script>
import Vue from 'vue'
import {store} from "../../store"
import {UtilFunctions, PROXY, CHANNEL, ASBTS_USERPROPS} from '../../common/http-common'
import VueFlashMessage from 'vue-flash-message';
Vue.use(VueFlashMessage);
require('vue-flash-message/dist/vue-flash-message.min.css')
const cloneDeep = require('clone-deep')
import { ButtonGroupPlugin, ButtonPlugin, ToastPlugin } from 'bootstrap-vue'
Vue.use(ButtonGroupPlugin)
Vue.use(ButtonPlugin)
Vue.use(ToastPlugin)
import SignIn from "../SignIn";
import testSessionMixin from "../../mixins/testSessionMixin";

export default {
  data () {
    return {
      channel: null,  // channel object
      edit: false,
      originalChannelName: null,   // in case of delete
      discarding: false,  // for saving edits
      ackMode: false,  // for deleting
      lockAckMode: "", // for locking configuration to prevent unauthorized edits
      badNameMode: false,
      badNameModeReason: null,
      editUserProps: ASBTS_USERPROPS,
    }
  },
  props: [
    'sessionId',
    'channelName',
      'startEdit'
  ],
  created() {
      // console.log('In Cedit.')
      try {
        this.fetch()
        this.showAck(true)
        if (this.channelIsNew) {
          this.edit = true
        }
      } catch (e) {
       console.log('error in Created lifecycle event: ' + e )
      }
  },
  watch: {  // when $route changes run fetch()
    $route: function() {
      this.fetch();
    },
  },
  computed: {
    channelId() {
      return this.sessionId + '__' + this.channelName;
    },
    channelIsNew() {
        return this.$store.state.base.channelIsNew;
    },
    isEditMode() {
      return this.edit || this.channelIsNew
    },
    isHttpsMode() {
      return UtilFunctions.isHttpsMode()
    },
    channelMhdVersionSupport: {
      set(val)
      {
        if (val === '' || val === undefined)
          return
       this.channel.mhdVersions = [val]
      },
      get()
      {
        if (this.channel.mhdVersions !== undefined && Array.isArray(this.channel.mhdVersions) && this.channel.mhdVersions.length > 0) {
          return this.channel.mhdVersions[0]
        } else {
            return ''
        }
      }
    }
  },
  mounted() {
  },
  components: {
    SignIn
  },
  methods: {
    msg(msg) {
      console.log(msg)
      this.$bvToast.toast(msg, {noCloseButton: true})
    },
    error(msg) {
      console.log(msg)
      this.$bvToast.toast(msg, {noCloseButton: true, title: 'Error'})
    },
    requestDelete() {
      this.ackMode = true
    },
    deleteAcked() {
      this.deleteChannel()
      this.ackMode = false
      const route = '/session/' + this.channel.testSession + '/channels'
      this.channel = undefined
      this.$router.push(route)
    },
    deleteCanceled() {
      this.ackMode = false
    },
    badNameCanceled() {
      this.badNameMode = false
    },
    getHidden() {
      return this.ackMode ? null : 'hidden'
    },
    showAck(bool) {
      if (bool) {
        this.ackHidden = null
      } else {
        this.ackHidden = ''
      }
    },
    copy() {  // actually duplicate (a channel)
      let chan = this.copyOfChannel()
      chan.channelName = 'copy'
      chan.writeLocked = false
      this.$store.commit('installChannel', chan)
      this.$store.commit('setChannelIsNew', true);
      this.edit = true
      this.$router.push('/session/' + this.sessionId + '/channels/copy')
    },
    toggleEdit() {
      this.edit = !this.edit
    },
    async save() {
      if (this.channelIsNew) {
        let url = ''
          try {
            if (this.isSessionConfigLocked) {
                if (this.isCanAddChannel) {
                  url = 'create'
                  await CHANNEL.post(url, this.channel)
                } else {
                  if (this.editUserProps.signedIn) {
                    url = 'accessGuard/channel/create'
                    await PROXY.post(url, this.channel, {
                      auth: {
                        username: this.editUserProps.bauser,
                        password: this.editUserProps.bapw
                      }
                    })
                  } else {
                    this.msg("Sign-in is required to add a new channel to a locked test session.")
                  }
                }
            } else {
              url = 'create'
              await CHANNEL.post(url, this.channel)
            }
            this.$store.commit('installChannel', this.channel)
            this.$store.commit('setChannelIsNew', false);
            this.edit = false
            this.lockAckMode = ""
            this.fetch(true)
            await this.$store.dispatch('loadChannelIds')
            this.msg('Saved.')
          }
          catch (error) {
            this.error(url + ': ' + error)
            this.edit = false
            this.lockAckMode = ""
          }
        } else {
          if (! this.channel.writeLocked) {
            try {
              await CHANNEL.put(`${this.channel.testSession}__${this.channel.channelName}`, this.channel);
              this.$store.commit('installChannel', this.channel)
              this.msg('Updated.')
              this.edit = false
              this.lockAckMode = ""
              this.fetch(true)
              await this.$store.dispatch('loadChannelIds')
            } catch (error) {
              if (error !== null && error !== undefined) {
                const hasResponse = Object.keys(error).indexOf('response')
                if (hasResponse) {
                  this.error(error.response.statusText)
                  this.error(error.response.data)
                }
              }
            }
          } else {  // has write lock
            let url = `/accessGuard/channel/${this.channel.testSession}__${this.channel.channelName}`
            try {
              await PROXY.put(url, this.channel, {
                auth: {
                  username: this.editUserProps.bauser,
                  password: this.editUserProps.bapw
                }
              })
              this.msg('Updated.')
              this.$store.commit('setChannelIsNew', false);
              this.edit = false
              this.lockAckMode = ""
              this.fetch(true)
              await this.$store.dispatch('loadChannelIds')
            } catch (error) {
              this.error(url + ': ' + error)
              this.edit = false
              this.lockAckMode = ""
            }
          }
        }
    },
    discard() {
        if (this.channelIsNew) {
            this.msg('Discarded.')
            this.edit = false
            this.$store.commit('setChannelIsNew', false);
            this.discarding = true
            const route = '/session/' + this.channel.testSession + '/channels'
            this.channel = undefined
            this.$router.push(route)
        } else {
            this.fetch(true)
            this.edit = false
            this.discarding = true
        }
    },
    isCurrentChannelIdNew() {
      return this.channel.channelName === 'new' || this.channel.channelName === 'copy'
    },
    isCurrentChannelIdBadPattern() {
      const name = this.channel.channelName
      const re = RegExp('^([a-zA-Z0-9_]+)$')
      const match = re.test(name)
      const re2 = RegExp('.*__.*')
      const match2 = re2.test(name)
      return !match || match2
    },
    /*
    Should not the base store for the new channel flag be used for this?
    isNewChannelId() {
      return this.channelName === 'new' || this.channelName === 'copy'
    },
     */
    isPreloaded() {
      const channel = this.$store.state.base.channel;
      // console.log('int. channel name: ' + this.channelName + '. ext channel name: ' + channel.channelName)
      // console.log('int ts: ' + this.sessionId + ' ext. ts: ' + channel.testSession)
      const ret = channel && this.channelName === channel.channelName && this.sessionId === channel.testSession;
      // console.log('isPreloaded: ' + ret)
      return ret
    },
    fetch(reload = false) {
      if (this.channelName === undefined)
        return
      this.originalChannelName = this.channelName
      this.lockCanceled()
      if (this.channelIsNew) {
        this.channel = this.copyOfChannel()
        this.discarding = false
        this.edit = true
      } else {
          if (this.$store.state.base.channelName !== this.channelName) {
            this.$store.commit('setChannelName', this.channelName)
          }
      }

      if (! reload) {
        const isPreloaded = this.isPreloaded()
        if (isPreloaded) {
          this.discarding = false;
          this.channel = this.copyOfChannel();
          return
        }
      }

      this.channel = null
      const fullId = this.channelId;

      this.$store.dispatch('loadChannel', {channelId: fullId, raiseFtkCommit: false})
          .then(channel => {
            this.channel = cloneDeep(channel)
            this.discarding = false
          })
    },
    getChannel() {
      return this.$store.state.base.channel
    },
    copyOfChannel() {
      const chan = this.getChannel()
      return cloneDeep(chan)
    },
    getChannelBase(https, channel) {
      if (https)
        return UtilFunctions.getHttpsChannelBase(channel)
      else
        return UtilFunctions.getChannelBase(channel)
    },
    requestLock(boolIn) {
      const bool = boolIn
      const that = this
      this.lockAcked = function() {
        that.lockChannel(boolIn).then (response => {
          if (response) {console.log(response)}
        })
      }
      // If signedIn, directly run the method
      if (this.editUserProps.signedIn) {
        this.lockAcked()
      } else {
        // If not signed In, show the signIn component
        this.lockAckMode = (bool?"Lock":"Unlock") + " Configuration:";
      }
    },
    lockAcked() {},
    lockCanceled() {
      this.lockAckMode = ""
      this.lockAcked = null
      this.edit = false
    },
    async lockChannel(boolIn) {
      const bool = boolIn
      const that = this
      let chan = cloneDeep(this.channel)
      chan.writeLocked = bool
      await PROXY.post('channelLock', chan, { auth: {username: this.editUserProps.bauser, password: this.editUserProps.bapw}})
          .then(function () {
            that.channel.writeLocked = bool
            that.lockAckMode = ""
            that.$store.commit('installChannel', that.channel)
            that.msg('Channel configuration is ' + ((bool)?'locked':'unlocked'))
          })
          .catch(function (error) {
            let msg = ((error && error.response && error.response.status && error.response.statusText) ? (error.response.status +  ': ' + error.response.statusText) : "")
            if (msg)
              that.error({message: msg})
          })
    },
    guardedFn(str, fn) {
      if (typeof fn === 'function') {
        if (this.channel.writeLocked) {
          if (this.editUserProps.signedIn) {
            this.lockAcked = null
            fn.call()
          } else {
            const that = this
            this.lockAcked = function() {that.guardedFn(str, fn)}
            this.lockAckMode = str + ": "
          }
        } else {
          fn.call()
        }
      }
    }
  },
  mixins: [testSessionMixin],
  store: store,
  name: "ChannelEdit"
}
</script>

<style scoped>
.window {
  display: grid;
  grid-template-columns: auto auto;
  margin: 5px;
}
.grid-container {
  display: grid;
  grid-template-columns: auto;
  grid-template-rows: auto;
  grid-column-gap: 10px;
}
.grid-name {
  /*font-weight: bold;*/
  /*background-color: rgba(255, 255, 255, 0.8);*/
  grid-column: 1;
  text-align: left;
  margin-bottom: 2px;
}
.grid-item {
  /*background-color: rgba(255, 255, 255, 0.8);*/
  grid-column: 2;
  text-align: left;
  margin-bottom: 2px;
}
.divider{
  width:5px;
  height:auto;
  display:inline-block;
}
.ok-button {
  font-size: 15px;
  padding: 0px 0px;
}
.cancel-button {
  font-size: 15px;
  padding: 0px 0px;
}
.button-bar {
  grid-column: 0 / span 2;
  alignment: left;
  margin-bottom: 10px;
}
.tooltip {
  position: relative;
  display: inline-block;
  /*border-bottom: 1px dotted black;*/
}

.tooltip .tooltiptext {
  visibility: hidden;
  width: 120px;
  background-color: blue;
  color: #fff;

  bottom: 100%;
  left: 50%;
  margin-left: -60px;

  /* Position the tooltip */
  position: absolute;
  z-index: 1;
}

.tooltip:hover .tooltiptext {
  visibility: visible;
}
.channelError {
  color: red;
  text-align: left;
  /*border: 1px dotted black;*/
  grid-column: 1 / span 2;
}
.caption {
  font-weight: bold;
  font-size: larger;
}
.center {
  text-align: left;
  margin-left: 50px;
}
</style>
