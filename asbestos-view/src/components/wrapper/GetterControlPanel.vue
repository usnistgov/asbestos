<template>
    <div>
        <div class="control-panel-item-title" @click="launch()">GET/Inspect/Validate</div>
        <div class="selectable" @click="launch()">Launch</div>
    </div>
</template>

<script>
    export default {
        methods: {
            launch() {
                this.$router.push(`/session/${this.$store.state.base.channel.testSession}/channel/${this.$store.state.base.channel.channelName}/getter`)
            }
        },
        name: "GetterControlPanel"
    }
</script>

<style scoped>

</style>
