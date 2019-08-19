<template>
    <div>
        Channels
        <img @click="pushNewChannelRoute" src="../assets/add-button.png"/>
        <div v-for="(channelId, index) in $store.state.base.channelIds" :key="channelId">
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
            'channelId'
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
                const newId = this.$store.state.base.channelIds.length - 1
                return '/session/' + this.$route.params.sessionId + '/channel/' + newId
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
