<template>
    <div>
      <div>Top Layout</div>
        <router-link to="/session/default/channel">Go To Channels</router-link><br />
        <router-link to="/test">Go To Test</router-link>
<!--        <ToolBody></ToolBody>-->
<!--        <ChannelsView></ChannelsView>-->
        <router-view></router-view>
    </div>
</template>

<script>

    // import ToolBody from "@/components/ToolBody.vue"
    // import ChannelsView from "@/components/ChannelsView.vue"
    export default {
        name: 'TopLayout',
        components: {
            // ToolBody,
            // ChannelsView
        }
    }
</script>
