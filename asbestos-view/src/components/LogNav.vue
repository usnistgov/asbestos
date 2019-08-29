<template>
    <div class="solid-boxed">
        <div class="tooltip">
            <img id="return-button" class="selectable" src="../assets/arrow-up.png" @click="up()"/>
            <span class="tooltiptext">Return</span>
        </div>
        <div class="divider"></div>
        <div class="divider"></div>
        <div class="tooltip">
            <img id="left-button" class="selectable" src="../assets/left-arrow.png" @click="left()"/>
            <span class="tooltiptext">Previous</span>
        </div>
        <div class="divider"></div>
        <div class="divider"></div>
        <div class="tooltip">
            <img id="right-button" class="selectable" src="../assets/right-arrow.png" @click="right()"/>
            <span class="tooltiptext">Next</span>
        </div>
        Item {{ index + 1 }} of {{ this.$store.state.base.eventSummaries.length }}
    </div>
</template>

<script>

    export default {
        data() {
            return {

            }
        },
        methods: {
            up() {
                this.$router.back()
            },
            left() {
                if (this.$store.state.base.currentEventIndex > 0) {
                    this.$store.commit('updateCurrentEventIndex', -1)
                    this.updateRoute()
                }
            },
            right() {
                if (this.$store.state.base.currentEventIndex + 1 < this.$store.state.base.eventSummaries.length) {
                    this.$store.commit('updateCurrentEventIndex', 1)
                    this.updateRoute()
                }
            },
            updateRoute() {
                const index = this.$store.state.base.currentEventIndex
                const summary = this.$store.state.base.eventSummaries[index]
                this.$router.replace(`/session/${this.sessionId}/channel/${this.channelId}/lognav/${summary.eventName}`)
            },
        },
        created() {
            this.$store.commit('setCurrentEventIndex', this.index)
        },
        props: [
            'index', 'sessionId', 'channelId'
        ],
        components: {

        },
        name: "LogNav"
    }
</script>

<style scoped>

</style>
