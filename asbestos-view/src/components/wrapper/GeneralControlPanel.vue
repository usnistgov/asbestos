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
                if (! this.disabled) {
                    const route = `/session/${this.$store.state.base.channel.testSession}/channel/${this.$store.state.base.channel.channelName}/logs`
                    this.$router.push(route)
                }
            },
            getter() {
                if (! this.disabled) {
                    this.$router.push(`/session/${this.$store.state.base.channel.testSession}/channel/${this.$store.state.base.channel.channelName}/getter`)
                }
            },
        },
        props: [
            'disabled'
        ],
        name: "GeneralControlPanel"
    }
</script>

<style scoped>

</style>
