<template>
    <div>
        Channels
        <div v-for="(channelId, index) in $store.state.base.channelIds" :key="channelId">
            <router-link class="element-nav" v-bind:to="channelLink(index)">
                {{ channelName(channelId) }}
            </router-link>
        </div>
    </div>
</template>

<script>
    import axios from 'axios'
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
                        this.$store.commit('installChannelIds', response.data.sort())
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
