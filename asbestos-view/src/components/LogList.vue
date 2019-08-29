<template>
    <div>
        <span class="tool-title">Log List for {{ resourceType }}</span>
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

    export default {
        data() {
            return {
                monthNames: [ 'Jan', 'Feb', 'Mar', 'April', 'May', 'June', 'July', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec'],
                eventSummaries: [],
                eventSummariesByType: [],   // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false }
                selectedEventName: null,
                selectedEvent: null,
                selectedTask: 0,
            }
        },
        methods: {
            loadEventSummaries() {
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
                    this.eventSummariesByType =  summaries.filter(item => {
                        return item.resourceType === type
                    }).sort((a, b) => a.eventName > b.eventName ? -1 : 1)
                }
            },
            selectSummary(summary) {
                this.$store.commit('setEventSummaries', this.eventSummaries)
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/lognav/${summary.eventName}`)
            },
            eventAsDate(name) {
                const parts = name.split('_')
                // const year = parts[0]
                const month = parts[1]
                const day = parts[2]
                const hour = parts[3]
                const minute = parts[4]
                const second = parts[5]
                const milli = parts[6]
                const monthName = this.monthNames[+month]
                //return name
                return `${day} ${monthName} ${hour}:${minute}:${second}:${milli}`
            },
            msg(msg) {
                console.log(msg)
                this.$bvToast.toast(msg, {noCloseButton: true})
            },
            error(err) {
                this.$bvToast.toast(err.message, {noCloseButton: true, title: 'Error'})
                console.log(err)
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
        name: "LogList"
    }
</script>

<style scoped>

</style>
