<template>
    <div>
        <span class="tool-title">Logs for Channel {{ channelId }}</span>
        <span class="divider"></span>

        <img id="reload" class="selectable" @click="loadEventSummaries()" src="../assets/reload.png"/>
        <span class="divider"></span>

        <div v-for="(eventSummary, i) in eventSummaries"
             :key="eventSummary.eventName + i">
            <div >
                <div class="summary-label boxed has-cursor"
                     @click="selectSummary(eventSummary)">
                    {{ eventAsDate(eventSummary.eventName) }} - {{ eventSummary.verb}} {{ eventSummary.resourceType }} - {{ eventSummary.status ? 'Ok' : 'Error' }}
                </div>

            </div>
        </div>
    </div>
</template>

<script>
    import eventMixin from '../mixins/eventMixin'
    import errorHandlerMixin from '../mixins/errorHandlerMixin'

    export default {
        data() {
            return {
                //eventSummaries: [],  // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false }
                eventSummariesByType: [],
                selectedEventName: null,
                selectedEvent: null,
                selectedTask: 0,
            }
        },
        methods: {
            selectSummary(summary) {
                //this.$store.commit('setEventSummaries', this.eventSummaries)
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/lognav/${summary.eventName}`)
            },
            loadEventSummaries() {
                this.$store.dispatch('loadEventSummaries')
            }
        },
        created() {
            this.loadEventSummaries()
        },
        computed: {
            eventSummaries() {
                return this.$store.state.log.eventSummaries
            },
        },
        watch: {
            'resourceType': 'loadEventSummaries'
        },
        props: [
            'resourceType',  'sessionId', 'channelId',
        ],
        mixins: [eventMixin, errorHandlerMixin],
        name: "LogList"
    }
</script>

<style scoped>

</style>
