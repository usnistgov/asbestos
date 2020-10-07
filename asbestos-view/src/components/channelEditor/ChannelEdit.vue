<template xmlns:v-slot="http://www.w3.org/1999/XSL/Transform">
  <div>
    <!-- begin grid -->
    <div class="window">
      <div v-if="channel" class="grid-container">
        <div class="button-bar">
          <div v-if="edit || channelIsNew">
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
<!--            <span v-if="channelIsNew" class="right">Edited</span>-->
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
              <div class="tooltip">
                <img id="select-button" src="../../assets/select.png" @click="select()"/>
                <span class="tooltiptext">Select</span>
              </div>
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
<!--              <div class="tooltip">-->
<!--                <img id="delete-button" src="../../assets/delete-button.png" @click="guardedFn('Delete',requestDelete)" />-->
<!--                <span class="tooltiptext">Delete</span>-->
<!--              </div>-->
<!--              <div class="divider"></div>-->
<!--              <div class="divider"></div>-->
              <div v-if="channel.writeLocked" class="tooltip">
                <img id="unlock-button" src="../../assets/lock-icon.png" @click="requestLock(false)"/>
                <span class="tooltiptext">Configuration is locked.</span>
              </div>
              <div v-else class="tooltip">
                <img id="lock-button" src="../../assets/unlock-icon.png" @click="requestLock(true)"/>
                <span class="tooltiptext">Configuration is unlocked.</span>
              </div>
<!--              <span v-if="channelIsNew" class="right">Edited</span>-->
            </div>

          </div>

        </div>
        <label class="grid-name">Id</label>
        <div v-if="isNew" class="grid-item">
          <input v-model="channel.channelName">
        </div>
        <div v-else class="grid-item">{{ channel.channelName }}</div>

        <label class="grid-name">Test Session</label>
        <div class="grid-item">{{ channel.testSession }}</div>

        <label class="grid-name">Environment</label>
        <div v-if="edit" class="grid-item">
          <select v-model="channel.environment">
            <option v-for="e in $store.state.base.environments" :key="e">
              {{e}}
            </option>
          </select>
        </div>
        <div v-else class="grid-item">{{ channel.environment }}</div>

        <label class="grid-name">Channel Type</label>
        <div v-if="edit" class="grid-item">
          <select v-model="channel.channelType">
            <option v-for="ct in $store.state.channel.channelTypes" :key="ct">
              {{ct}}
            </option>
          </select>
        </div>
        <div v-else class="grid-item">{{ channel.channelType }}</div>

        <label class="grid-name">Fhir Base</label>
        <div v-if="edit" class="grid-item">
          <input v-model="channel.fhirBase">
          Only used with Channel Type fhir
        </div>
        <div v-else class="grid-item">{{ channel.fhirBase }}</div>

        <label class="grid-name">XDS Site Name</label>
        <div v-if="edit" class="grid-item">
          <input v-model="channel.xdsSiteName">
          Only used with Channel Type mhd
        </div>
        <div v-else class="grid-item">{{ channel.xdsSiteName }}</div>

        <label v-if="channel.channelType === 'mhd'" class="grid-name">Log MHD Capability Statement Request?</label>
        <div v-if="channel.channelType === 'mhd' && edit" class="grid-item">
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

        <div v-if="!lockAckMode && !edit && !channel.fhirBase && !channel.xdsSiteName" class="channelError">
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          Warning: FhirBase or XDS Site Name must be present
        </div>
        <div v-if="!lockAckMode && !edit && channel.channelType === 'fhir' && !channel.fhirBase" class="channelError">
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          Warning: FHIR type is selected but no FHIR Base is configured
        </div>
        <div v-if="!lockAckMode && !edit && channel.channelType === 'mhd' && !channel.xdsSiteName" class="channelError">
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          <div class="vdivider"></div>
          Warning: MHD type is selected but no XDS Site Name is configured
        </div>
      </div>
    </div>
    <!-- end of grid -->
    <div v-if="channel && !edit">
      <div>
        <p class="caption">Channel Base Address: </p>
        <span class="center">{{getChannelBase(false, channel)}}</span>
      </div>
      <div v-if="isHttpsMode()">
        <p class="caption">Optional HTTPS Channel Base Address: </p>
        <span class="center">{{getChannelBase(true, channel)}}</span>
      </div>
      <div>
        <p>Send to this URL and</p>
        <ul>
          <li>Proxy will record your transaction</li>
          <li>Proxy will forward your transaction to
            <span v-if="channel.fhirBase">{{channel.fhirBase}}</span>
            <span v-if="channel.xdsSiteName">XDS Toolkit site {{channel.xdsSiteName}}</span>
          </li>
        </ul>
      </div>
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

export default {
  data () {
    return {
      channel: null,  // channel object
      edit: false,
      isNew: false,
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
    'theChannel',
      'startEdit'
  ],
  created() {
    this.fetch()
    this.showAck(true)
    if (this.startEdit) {
      this.edit = true;
      this.isNew = true;
    }
    // this.loadChannelBaseAddr()
  },
  watch: {  // when $route changes run fetch()
    $route: function() {
      this.fetch();
    },
    channelName: function (newChannelName) {
      this.updateToChannel(newChannelName);
    },
    startEdit: function (newStartEdit) {
      this.startEdit = newStartEdit;
    },
    channelIsNew(value) {
      this.edit = value;
    }
  },
  computed: {
    channelIds: {
      get() {
        return this.$store.getters.getEffectiveChannelIds;
      },
    },
    channelId() {
      return this.sessionId + '__' + this.channelName;
    },
    channelIsNew() {
        return this.$store.state.base.channelIsNew;
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
      this.$bvToast.toast(msg.message, {noCloseButton: true, title: 'Error'})
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
      let chan = cloneDeep(this.channel)
      chan.channelName = 'copy'
      chan.writeLocked = false
      this.$store.commit('installChannel', chan)
      this.$router.push('/session/' + this.sessionId + '/channels/copy')
    },
    async deleteChannel() {
      try {
        if (! this.channel.writeLocked) {
          await PROXY.delete('channel/' + this.sessionId + '__' + this.channelName)
        } else if (this.editUserProps.bapw !== "") {
          await PROXY.delete('channelGuard/' + this.channelId, { auth: {username: this.editUserProps.bauser, password: this.editUserProps.bapw}})
        }
        this.msg('Deleted')
        this.$store.commit('deleteChannel', this.channelId)
        await this.$store.dispatch('loadChannelIds')
        this.$router.push('/session/' + this.sessionId + '/channels')
      } catch (error) {
        this.lockAckMode = ""
        this.error(error)
      }
    },

    toggleEdit() {
      this.edit = !this.edit
    },
    async save() {
      const that = this
      if (this.isNew) {
        console.log(`new new new`)
        if (this.isCurrentChannelIdNew()) {
          this.badNameMode = true
          this.badNameModeReason = `'new' is temporary and not acceptable`
          return
        }
        if (this.isCurrentChannelIdBadPattern()) {
          this.badNameMode = true
          this.badNameModeReason = `Name may only contain a-z A-Z 0-9 _  and __ not allowed`
          return
        }
        let response = await this.saveToServer(this.channel);
          if (response) {console.log(response)}
          this.$store.commit('installChannel', cloneDeep(this.channel))
          //this.$store.commit('deleteChannel', this.originalChannelName) // original has been renamed
          this.isNew = false
          this.edit = false
          await this.$store.dispatch('initSessionsStore');
          // this.$store.commit('setChannelName', this.channel.channelName);
          // this.$store.commit('setSession', this.channel.testSession);
          await this.$store.dispatch('loadChannelIds')
          this.$router.push('/session/' + this.channel.testSession + '/channels/' + this.channel.channelName)
      } else {
        this.$store.commit('installChannel', cloneDeep(this.channel))
        if (! this.channel.writeLocked) {
          const url = `CHANNEL/create`;
          CHANNEL.post('create', this.channel)
              .then(function () {
                that.msg('Updated')
                that.isNew = false
                that.edit = false
                that.lockAckMode = ""
                that.fetch()
                this.$store.dispatch('loadChannelIds')
              })
              .catch(function (error) {
                that.error(url + ': ' + error)
                that.isNew = false
                that.edit = false
              })
        } else {
          const url = `channelGuard/create`;
          PROXY.post('/channelGuard', this.channel, {
            auth: {
              username: this.editUserProps.bauser,
              password: this.editUserProps.bapw
            }
          })
              .then(function () {
                that.msg('Saved')
                that.isNew = false
                that.edit = false
                that.lockAckMode = ""
                that.fetch()
                this.$store.dispatch('loadChannelIds')
              })
              .catch(function (error) {
                that.error(url + ': ' + error)
                that.isNew = false
                that.edit = false
                that.lockAckMode = ""
              })
        }
      }

    },
    async saveToServer(aChannel) {
      const url = `CHANNEL/create`;
      try {
        console.log(`saveToServer`)
        await CHANNEL.post('create', aChannel)
        this.msg('New Channel Saved')
        await this.$store.dispatch('loadChannelNames')
        await this.$store.dispatch('loadChannelIds')
      } catch(error) {
        this.error(url + ': ' + 'saveToServer ' + error)
      }
    },
    discard() {
      if (this.isNew) {
        this.deleteChannel()
      }
      this.isNew = false
      this.toggleEdit()
      this.discarding = true
      const route = '/session/' + this.channel.testSession + '/channels'
      this.channel = undefined
      this.$router.push(route)
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
    isNewChannelId() {
      return this.channelName === 'new' || this.channelName === 'copy'
    },
    isPreloaded() {
      const channel = this.$store.state.base.channel;
      return channel && this.channelName === channel.channelName && this.sessionId === channel.testSession;
    },
    updateToChannel(channelName) {
      if (!channelName)
        return
      if (this.isPreloaded())
        return;
      this.channelName = channelName;
      this.$store.dispatch('loadChannel', this.channelId)   // this.channelId is computed off this.channelName
          .then(channel => {
            this.channel = channel
          })
    },
    fetch() {
      if (this.channelName === undefined)
        return
      this.originalChannelName = this.channelName
      this.lockCanceled()
      if (this.isNewChannelId()) {
        this.edit = true
        this.isNew = true
        this.channel = this.copyOfChannel()
        this.discarding = false
        return
      }

      console.log(`checking for preload`);
      const channel = this.$store.state.base.channel;
      if (this.isPreloaded()) {
        this.discarding = false;
        this.channel = channel;
        console.log(`preloaded`);
        return;
      }

      this.channel = null
      const fullId = this.channelId;

      if (this.theChannel)
        this.channel = this.theChannel;
      else {
        this.$store.dispatch('loadChannel', fullId)
            .then(channel => {
              this.channel = channel
            })
      }
      this.discarding = false
    },
    channelIndex(theSessionId, theChannelName) {
      const fullChannelId = `${theSessionId}__${theChannelName}`;
      return this.$store.state.base.channelIds.findIndex( function(channelId) {
        return channelId === fullChannelId
      })
    },
    getChannel() {
      return this.$store.state.base.channel
    },
    copyOfChannel() {
      const chan = this.getChannel()
      return cloneDeep(chan)
    },
    select() {
      if (this.channel.testSession === undefined || this.channel.channelName === undefined) {
        return
      }
      const newRoute =  '/session/' + this.channel.testSession + '/channel/' + this.channel.channelName
      this.$store.commit('setChannelName', this.channel.channelName)
      this.$router.push(newRoute)
    },
    isHttpsMode() {
      return UtilFunctions.isHttpsMode()
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
            that.msg('Channel configuration is ' + ((bool)?'locked':'unlocked'))
            that.lockAckMode = ""
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
