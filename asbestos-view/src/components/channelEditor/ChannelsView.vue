<template>
<!--    <div class="container">-->
<!--      <div class="channel-panel-header">-->
<!--        Channels-->
<!--        <div class="tooltip">-->
<!--          <img id="add-button" @click="pushNewChannelRoute()" src="../../assets/add-button.png"/>-->
<!--          <span class="tooltiptext">Add Channel</span>-->
<!--        </div>-->
<!--        <div v-for="(channelId) in channelIds" :key="channelId">-->
<!--          <router-link class="element-nav" v-bind:to="channelsLink(channelId)">-->
<!--            {{ channelId }}-->
<!--          </router-link>-->
<!--        </div>-->
<!--        <router-view></router-view>-->

<!--      </div>-->
<!--  <div class="container">-->
  <div>
        <channel-nav :session-id="sessionId" class="nav"></channel-nav>
<!--        <channel-edit :session-id="sessionId" :channelName="channelName" class="view"></channel-edit>-->
    </div>
</template>

<script>
    import ChannelNav from "./ChannelNav"
//    import ChannelEdit from "./ChannelEdit"
    import {newChannel} from "@/types/channel";

    export default {
        data() {
            return {

            }
        },
        components: {
            ChannelNav,
            //ChannelEdit
        },
        name: "ChannelsView",
        mounted() {
        },
      computed: {
        channelIds: {
          get() {
            return this.$store.getters.getChannelIdsForCurrentSession;
          }
        }
      },
        methods: {
          channelsLink(channelId) {
            const chan = channelId.split('__', 2);
            // const session = chan[0];
//            const
                this.channelName = chan[1];
            //return '/session/' + session + '/channels/' + channelName;
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
            this.$store.commit('setChannelIsNew');
            console.log(`newChannel ${Object.keys(chan)}`)
            return '/session/' + this.sessionId + '/channels/new'
          },
        },
        props: [
           'sessionId', 'channelName'
        ]
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
