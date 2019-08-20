<template>
    <div>
        Channels
        <img @click="pushNewChannelRoute" src="../assets/add-button.png"/>
        <div v-for="(channelId, index) in fullChannelIds()" :key="channelId">
            <router-link class="element-nav" v-bind:to="channelLink(index)">
                {{ channelName(channelId) }}
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
            'sessionId'
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
                const newId = this.fullChannelIds().length - 1
                return '/session/' + this.sessionId + '/channel/' + newId
            },
            channelName(id) {
                const sepat = id.indexOf('__')
                return id.substring(sepat+2)
            },
            channelLink(index) {
                return '/session/default/channel/' + index
            },
            loadChannelNames() {
                axios.get(`http://localhost:8081/proxy/channel`)
                    .then(response => {
                        let theResponse = response.data
                        this.$store.commit('installChannelIds', theResponse.sort())
                    })
                // .catch...
            },
            fullChannelIds() {
                return this.$store.state.base.fullChannelIds
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
