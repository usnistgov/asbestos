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
    import {LOG} from '../common/http-common'
    import eventMixin from '../mixins/eventMixin'
    import errorHandlerMixin from '../mixins/errorHandlerMixin'

    export default {
        data() {
            return {
                eventSummaries: [],  // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false }
                eventSummariesByType: [],
                selectedEventName: null,
                selectedEvent: null,
                selectedTask: 0,
            }
        },
        methods: {
            loadEventSummaries() {
                if (!this.sessionId) {
                    this.error('Session not set')
                    return
                }
                if (!this.channelId) {
                    this.error('Channel not set')
                    return
                }
                LOG.get(`${this.sessionId}/${this.channelId}`, {
                    params: {
                        summaries: 'true'
                    }
                })
                    .then(response => {
                        this.eventSummaries = response.data.sort((a, b) => {
                            if (a.eventName < b.eventName) return 1
                            return -1
                        })
                    })
                    .catch(error => {
                        this.error(error)
                    })
            },
            selectSummary(summary) {
                this.$store.commit('setEventSummaries', this.eventSummaries)
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/lognav/${summary.eventName}`)
            },

        },
        created() {
            this.loadEventSummaries()
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
