<template>
    <div>
        <div class="channel-panel-header">
            Channels
        </div>

        <div v-for="channelId in $store.state.base.channelIds" :key="channelId">
            Channel {{channelId}}
        </div>
    </div>
</template>

<script>
    export default {
        name: "ChannelPanel"
    }
</script>

<style scoped>
    .channel-panel-header {
        font-weight: bold;
    }
</style>
