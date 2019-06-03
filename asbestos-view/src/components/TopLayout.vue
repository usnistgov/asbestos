<template>
    <div>
      <div>Top Layout</div>
        <ToolBody></ToolBody>
        <router-view></router-view>
    </div>
</template>

<script>

    import ToolBody from "@/components/ToolBody.vue"
    export default {
        name: 'TopLayout',
        components: {
            ToolBody
        }
    }
</script>
