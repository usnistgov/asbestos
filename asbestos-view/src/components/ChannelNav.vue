<template>
    <div>
        <div class="channel-panel-header">
            Channels
        </div>

        <div v-for="channelId in $store.state.base.channelIds" :key="channelId">
            {{channelId}}
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
        name: "ChannelNav"
    }
</script>

<style scoped>
    .channel-panel-header {
        font-weight: bold;
    }
</style>
