<template>
    <div>
      <div>Top Layout</div>
        <ToolBody></ToolBody>
        <ChannelsView></ChannelsView>
        <router-view></router-view>
    </div>
</template>

<script>

    import ToolBody from "@/components/ToolBody.vue"
    import ChannelsView from "@/components/ChannelsView.vue"
    export default {
        name: 'TopLayout',
        components: {
            ToolBody,
            ChannelsView
        }
    }
</script>
