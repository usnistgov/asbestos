<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">Test Session</div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                channel: null,
                channels: null
            }
        },
        methods: {
            manage() {
                console.info('Manage test sessions')
                this.$router.push(`/session/${this.$store.state.base.session}/channel/${this.$store.state.base.channel}/testSession`)
            },
        },
        name: "TestSessionControlPanel"
    }
</script>

<style scoped>

</style>
