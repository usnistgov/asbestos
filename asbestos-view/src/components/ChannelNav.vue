<template>
    <div>
        <div class="channel-panel-header">
            Channels
        </div>

        <div v-for="(channelId, index) in $store.state.base.channelIds" :key="channelId">
            <router-link class="element-nav" v-bind:to="channelLink(index)">
                {{ channelId }}
            </router-link>

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
            'channelId'  // channelId - testsession/id
        ],
        components: { },
        methods: {
            channelLink(index) {
                return '/channel/' + index
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
