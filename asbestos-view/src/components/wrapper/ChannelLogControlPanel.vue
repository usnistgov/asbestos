<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">Events on this channel</div>
        <div class="selectable" @click="manage()">View</div>
        <div v-if="!selectable" class="not-available">Select FHIR Server</div>
    </div>
</template>

<script>
    export default {
        data() {
            return {

            }
        },
        methods: {
            manage() {
                if (!this.selectable)
                    return;
                const route = `/session/${this.$store.state.base.session}/channel/${this.$store.state.base.channelId}/logs`
                console.log(`Route to ${route}`)
                this.$router.push(route)
            },
        },
        computed: {
            selectable() {
                return this.$store.state.base.session !== null && this.$store.state.base.channelId !== null
            }
        },
        created() {

        },
        mounted() {
            console.info('ChannelLogControlPanel mounted')
        },
        watch: {

        },
        name: "ChannelLogControlPanel"
    }
</script>

<style scoped>
    .not-available {
        color: red;
    }

</style>
