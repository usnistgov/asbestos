<template>
    <div class="channel-panel-header">
        My Channels
        <div class="tooltip">
            <img id="add-button" @click="pushNewChannelRoute()" src="../assets/add-button.png"/>
            <span class="tooltiptext">Add Channel</span>
        </div>
        <div v-for="(channelId) in channelIds()" :key="channelId">
            <router-link class="element-nav" v-bind:to="channelLink(channelId)">
                {{ channelId }}
            </router-link>
        </div>
<!--        <b-tooltip target="add-button" title="Add channel"></b-tooltip>-->
        <router-view></router-view>
    </div>
</template>

<script>
    import {newChannel} from '../types/channel'
    import Vue from 'vue'
    import { TooltipPlugin, ToastPlugin } from 'bootstrap-vue'
    Vue.use(TooltipPlugin)
    import {PROXY} from '../common/http-common'
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
            console.info('ChannelNav mounted')
            this.loadChannelNames()
        },
        methods: {
            pushNewChannelRoute() {
                return this.$router.push(this.newChannelRoute())
            },
            newChannelRoute() {
                let chan = newChannel()
                chan.testSession = this.sessionId
                this.$store.commit('installChannel', chan)
                //const newId = this.fullChannelIds().length - 1
                return '/session/' + this.sessionId + '/channel/new'
            },
            channelName(id) {
                const sepat = id.indexOf('__')
                return id.substring(sepat+2)
            },
            sessionName(id) {
                const sepat = id.indexOf('__')
                return id.substring(0, sepat)
            },
            channelLink(channelId) {
                return '/session/' + this.sessionId + '/channel/' + channelId
            },
            loadChannelNames() {
                const that = this
                PROXY.get('channel')
                    .then(response => {
                        let theResponse = response.data
                        this.$store.commit('installChannelIds', theResponse.sort())
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
            },
            fullChannelIds() {  // only ones matching current session
                return this.$store.state.base.fullChannelIds.filter(id => this.sessionName(id) === this.sessionId)
            },
            channelIds() {
                return this.fullChannelIds().map(x => this.channelName(x)).sort()
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
