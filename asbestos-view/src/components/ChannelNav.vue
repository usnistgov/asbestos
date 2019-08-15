<template>
    <div>
        <div class="channel-panel-header">
            Channels
        </div>

        <div v-for="channelId in $store.state.base.channelIds" :key="channelId">
            <router-link class="element-nav" v-bind:to="channelLink(channelId)">
                {{ channelId }}
            </router-link>
            The Channel {{channelId}}
        </div>
    </div>
</template>

<script>
    export default {
        data() {
            return {

            }
        },
        props: [
            'channelId'  // channelId of channel currently being displayed
        ],
        components: { },
        methods: {
            channelLink(channelId) {
                return '/channel/' + channelId
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
