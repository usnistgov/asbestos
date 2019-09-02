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
                eventSummaries: [],
                eventSummariesByType: [],   // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false }
                selectedEventName: null,
                selectedEvent: null,
                selectedTask: 0,
            }
        },
        methods: {
            loadEventSummaries() {
                if (this.sessionId === null) {
                    this.error('Session not set')
                    return
                }
                if (this.channelId === null) {
                    this.error('Channel not set')
                    return
                }
                LOG.get(`${this.sessionId}/${this.channelId}`, {
                    params: {
                        summaries: 'true'
                    }
                })
                    .then(response => {
                        this.eventSummaries = response.data
                        let types = []
                        this.eventSummaries.forEach(summary => {
                            if (!types.includes(summary.resourceType))
                                types.push(summary.resourceType)
                        })
                        types.push('All')
                        this.resourceTypes = types.sort()
                        console.log(`loaded ${response.data.length} summaries and ${types.length} types`)
                    })
                    .catch(error => {
                        this.error(error)
                    })
            },
            updateEventSummariesByType() {  // called by watcher when currentType is updated
                const type = this.resourceType
                const summaries = this.eventSummaries
                console.log(`updateEventSummariesByType(${type})`)// All is possible value plus anything in resourceTypes
                if (type === 'All') {
                    this.eventSummariesByType = summaries.sort((a, b) => a.eventName > b.eventName ? -1 : 1)
                } else {
                    console.log(`filter by ${type}`)
                    this.eventSummariesByType =  summaries.filter(item => {
                        return item.resourceType === type
                    }).sort((a, b) => a.eventName > b.eventName ? -1 : 1)
                }
            },
            selectSummary(summary) {
                this.$store.commit('setEventSummaries', this.eventSummaries)
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/lognav/${summary.eventName}`)
            },

        },
        created() {
            this.loadEventSummaries()
            this.updateEventSummariesByType()
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
