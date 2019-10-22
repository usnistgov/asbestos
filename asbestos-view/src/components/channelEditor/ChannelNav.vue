<template>
    <div class="channel-panel-header">
        Channels
        <div class="tooltip">
            <img id="add-button" @click="pushNewChannelRoute()" src="../../assets/add-button.png"/>
            <span class="tooltiptext">Add Channel</span>
        </div>
        <div v-for="(channelId) in channelIds()" :key="channelId">
            <router-link class="element-nav" v-bind:to="channelsLink(channelId)">
                {{ channelId }}
            </router-link>
        </div>
<!--        <b-tooltip target="add-button" title="Add channel"></b-tooltip>-->
        <router-view></router-view>
    </div>
</template>

<script>
    import {newChannel} from '../../types/channel'
    import Vue from 'vue'
    import { TooltipPlugin, ToastPlugin } from 'bootstrap-vue'
    Vue.use(TooltipPlugin)
  //  import {PROXY} from '../common/http-common'
    Vue.use(ToastPlugin)

    export default {
        data() {
            return {

            }
        },
        props: [
            'sessionId'
        ],
        components: { },
        mounted() {
        },
        methods: {
            // for create a new channel
            pushNewChannelRoute() {
                return this.$router.push(this.newChannelRoute())
            },
            newChannelRoute() {
                let chan = newChannel()
                chan.testSession = this.sessionId
                chan.channelId = 'new'
                chan.channelType = 'fhir'
                this.$store.commit('setChannel', chan)
                return '/session/' + this.sessionId + '/channels/new'
            },
            channelsLink(channelId) {
                return '/session/' + this.sessionId + '/channels/' + channelId
            },
            channelIds() {
                return this.$store.state.base.channelIds
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
    .channel-panel-header {
        font-weight: bold;
    }
    .element-nav {
        position: relative;
        left: 0px;
    }
</style>
