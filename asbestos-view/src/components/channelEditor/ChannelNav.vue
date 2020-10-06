<template>
  <div class="container">
    <div class="nav channel-panel-header">
      Channels
      <div class="tooltip">
        <img id="add-button" @click="adding=true" src="../../assets/add-button.png"/>
        <img id="delete" src="../../assets/exclude-button-red.png" @click="del()"/>
        <span class="tooltiptext">Add Channel</span>
      </div>
      <div>
        <div v-if="channelId">
          <select v-model="channelId" size="10">
            <option v-for="(chann, channI) in channelIds"
                    v-bind:value="chann"
                    :key="chann + channI"
            >
              {{ chann}}
            </option>
          </select>
        </div>
        <div v-if="adding">
          <input v-model="newChannelName">
          <button @click="doAdd()">Add</button>
          <button @click="cancelAdd()">Cancel</button>
        </div>
        <div v-if="deleting">
          Are you sure you want to delete {{channelId}}?
        </div>
        <div v-if="deleting">
          <button @click="confirmDel()">Delete</button>
          <button @click="cancelDel()">Cancel</button>
        </div>

      </div>
      <!--    <router-view></router-view>-->
    </div>
    <div v-if="channel" class="view">
      <channel-edit :session-id="sessionId" :channel-name="channelName" :new-channel="channel" class="view"></channel-edit>
    </div>
  </div>
</template>

<script>
import {newChannel} from '@/types/channel'
import ChannelEdit from "./ChannelEdit"
import Vue from 'vue'
import { TooltipPlugin, ToastPlugin } from 'bootstrap-vue'
import {ASBTS_USERPROPS, PROXY} from "@/common/http-common";
Vue.use(TooltipPlugin)
Vue.use(ToastPlugin)

export default {
  data() {
    return {
      channelIds: null,
      channelId: null,
      adding: false,
      newChannelName: null,
      channel: null,
      deleting: false,
      editUserProps: ASBTS_USERPROPS,
    }
  },
  props: [
    'sessionId'
  ],
  components: { ChannelEdit },
  mounted() {
  },
  created() {
    this.updateChannelIds();
    this.selectFirstChannelId();
    this.updateChannel();
  },
  watch: {
    channelId: function() {
      this.updateChannel();
    }
  },
  computed: {
    channelName() {
      if (this.channelId.split('__').length === 2)
        return this.channelId.split('__')[1];
      else
        return null;
    }
  },
  methods: {
    selectFirstChannelId() {
      if (this.channelIds && this.channelIds.length > 0)
        this.channelId = this.channelIds[0];
      else
        this.channelId = null;
    },
    async confirmDel() {
      try {
        if (! this.channel.writeLocked) {
          await PROXY.delete('channel/' + this.sessionId + '__' + this.channelName)
        } else if (this.editUserProps.bapw !== "") {
          await PROXY.delete('channelGuard/' + this.channelId, { auth: {username: this.editUserProps.bauser, password: this.editUserProps.bapw}})
        }
        this.msg('Deleted')
        this.$store.commit('deleteChannel', this.channelId)
        await this.$store.dispatch('loadChannelIds')
        this.selectFirstChannelId();
        //this.$router.push('/session/' + this.sessionId + '/channels')
      } catch (error) {
        this.error(error);
      }
      this.adding = false;
      this.deleting = false;
    },

    cancelDel() {
      this.adding = false;
      this.deleting = false;
    },

    del() {
      this.deleting = true;
    },
    updateChannel() {
      if (!this.channel || this.channel.channelName !== this.channelName) {
        this.$store.dispatch('loadChannel', this.channelId)
            .then(channel => {
              this.channel = channel
            })
      }
    },
    doAdd() {
      this.channel = newChannel();
      this.channel.channelName = this.newChannelName;
      this.$store.commit('setChannel', this.channel)
      this.adding = false;
      this.channelId = `${this.sessionId}__${this.newChannelName}`;
      if (this.channelIds === null)
        this.channelIds = [];
      this.channelIds.push(this.channelId);
    },
    cancelAdd() {
      this.newChannelName = null;
      this.adding = false;
      this.deleting = false;
    },
    updateChannelIds() {
      this.channelIds = this.$store.getters.getChannelIdsForCurrentSession;
    },
    // for create a new channel
    pushNewChannelRoute() {
      return this.$router.push(this.newChannelRoute())
    },
    newChannelRoute() {
      let chan = newChannel()
      chan.testSession = this.sessionId
      chan.channelName = 'new'
      chan.channelType = 'fhir'
      this.$store.commit('setChannel', chan)
      return '/session/' + this.sessionId + '/channels/new'
    },
    channelsLink(channelId) {
      const chan = channelId.split('__', 2);
      const session = chan[0];
      const channelName = chan[1];
      return '/session/' + session + '/channels/' + channelName;
    },
    msg(msg) {
      console.log(msg)
      this.$bvToast.toast(msg, {noCloseButton: true})
    },
    error(err) {
      this.$bvToast.toast(err.message, {noCloseButton: true, title: 'Error'})
      console.log(err)
    },
  },
  name: "ChannelNav"
}
</script>

<style scoped>
.container {
  display: grid;
  grid-template-columns: auto auto;
  grid-template-areas: 'nav view';
  align-content: start;
}
.nav {
  grid-area: nav;
  border: 1px dotted black;
  text-align: left;
  width: fit-content;
}
.view {
  grid-area: view;
  text-align: left;
  width: fit-content;
  /*border: 1px dotted black;*/
}

.channel-panel-header {
  font-weight: bold;
}
.element-nav {
  position: relative;
  left: 0px;
}
</style>
