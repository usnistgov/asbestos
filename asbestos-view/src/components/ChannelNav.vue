<template>
    <div>
        <div class="channel-panel-header">
            ChannelNav
        </div>

        <div v-for="(channelId, index) in $store.state.base.channelIds" :key="channelId">
            <router-link class="element-nav" v-bind:to="channelLink(index)">
                {{ channelId }}
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
            channelLink(index) {
                return '/session/default/channel/' + index
            },
            loadChannelNames() {
                axios.get(`http://localhost:8081/proxy/channel`)
                    .then(response => {
                        this.$store.commit('installChannelIds', response.data)
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
