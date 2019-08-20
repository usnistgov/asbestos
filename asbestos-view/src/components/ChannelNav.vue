<template>
    <div>
        Channels
        <img @click="pushNewChannelRoute()" src="../assets/add-button.png"/>
        <div v-for="(channelId) in channelIds()" :key="channelId">
            <router-link class="element-nav" v-bind:to="channelLink(channelId)">
                {{ channelId }}
            </router-link>
        </div>
    </div>
</template>

<script>
    import axios from 'axios'
    import {newChannel} from '../types/channel'

    export default {
        data() {
            return {

            }
        },
        props: [
//            'channelId',
            'sessionId'  // default value - display testSession only if it does not match
        ],
        components: { },
        mounted() {
            this.loadChannelNames()
        },
        methods: {
            pushNewChannelRoute() {
                return this.$router.push(this.newChannelRoute())
            },
            newChannelRoute() {
                this.$store.commit('installChannel', newChannel())
                //const newId = this.fullChannelIds().length - 1
                return '/session/default/channel/new'
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
                axios.get(`http://localhost:8081/proxy/channel`)
                    .then(response => {
                        let theResponse = response.data
                        this.$store.commit('installChannelIds', theResponse.sort())
                    })
                // .catch...
            },
            fullChannelIds() {  // only ones matching current session
                return this.$store.state.base.fullChannelIds.filter(id => this.sessionName(id) === this.sessionId)
            },
            channelIds() {
                return this.fullChannelIds().map(x => this.channelName(x))
            }
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
