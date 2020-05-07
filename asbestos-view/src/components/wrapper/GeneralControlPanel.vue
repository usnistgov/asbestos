<template>
    <div>
        <div class="control-panel-item-title">Tools</div>
        <div class="selectable" @click="events()">Events on selected Channel</div>
        <div class="selectable" @click="getter()">GET/Inspect/Validate</div>
    </div>
</template>

<script>
    export default {
        methods: {
            events() {
                const route = `/session/${this.$store.state.base.session}/channel/${this.$store.state.base.channelId}/logs`
                this.$router.push(route)
            },
            getter() {
                this.$router.push(`/session/${this.$store.state.base.session}/channel/${this.$store.state.base.channelId}/getter`)
            },
        },
        name: "GeneralControlPanel"
    }
</script>

<style scoped>

</style>
