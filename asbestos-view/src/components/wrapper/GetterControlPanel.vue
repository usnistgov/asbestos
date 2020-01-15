<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">GET Tool</div>
        <div class="selectable" @click="launch()">Launch</div>
    </div>
</template>

<script>
    export default {
        methods: {
            launch() {
                this.$router.push(`/session/${this.$store.state.base.session}/channel/${this.$store.state.base.channelId}/getter`)
            }
        },
        name: "GetterControlPanel"
    }
</script>

<style scoped>

</style>