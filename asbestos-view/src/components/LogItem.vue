<template>
    <div>
        <log-nav :index="index" :sessionId="sessionId" :channelId="channelId"></log-nav>

        <div class="event-description">
            {{ eventAsDate(eventSummary.eventName) }} - {{ eventSummary.verb}} {{ eventSummary.resourceType }} - {{ eventSummary.status ? 'Ok' : 'Error' }}
        </div>
        <div class="request-response">
        <span class="selectable"
              @click="displayRequest = true">Request</span>
            <div class="divider"></div>
            <span class="selectable"
                  @click="displayRequest = false">Response</span>
        </div>
        <div v-if="getEvent()">
            <div v-if="displayRequest" class="event-details">
                            <pre>{{ requestHeader }}
                            </pre>
                <pre>{{ requestBody }}</pre>
            </div>
            <div v-if="!displayRequest" class="event-details">
                            <pre>{{ responseHeader }}
                            </pre>
                <pre>{{ responseBody }}</pre>
            </div>
        </div>
    </div>
</template>

<script>
    import LogNav from "./LogNav"
    import {LOG} from '../common/http-common'

    export default {
        data() {
            return {
                monthNames: [ 'Jan', 'Feb', 'Mar', 'April', 'May', 'June', 'July', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec'],
                index: 0,
                selectedEvent: null,
                selectedTask: 0,
                displayRequest: true,
            }
        },
        methods: {
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
            findEventInStore() {
                return this.$store.state.base.eventSummaries.findIndex(summary => this.eventId === summary.eventName)
            },
            updateIndex() {
                this.index = this.findEventInStore()
            },
            selectedEventName() {
                return this.selectedEvent === null ? null : this.selectedEvent.eventName
            },
            getEvent() {
                this.loadEvent()
                return this.selectedEvent
            },
            async loadEvent() {
                console.log('loadEvent')
                const index = this.$store.state.base.currentEventIndex
                const summary = this.$store.state.base.eventSummaries[index]
                // don't reload if it is already the selected event
                const selectedEventName = summary.eventName === this.selectedEventName() ? null: summary.eventName
                if (selectedEventName !== null) {
                    this.selectedEvent = null
                    this.selectedTask = 0
                    console.log(`GET ${this.sessionId}/${this.channelId}/${summary.resourceType}/${summary.eventName}`)
                    await LOG.get(`${this.sessionId}/${this.channelId}/${summary.resourceType}/${summary.eventName}`)
                        .then(response => {
                            try {
                                this.selectedEvent = response.data
                            } catch (error) {
                                this.error(error)
                            }
                        })
                        .catch(error => {
                            this.error(error)
                        })
                }
            },
            msg(msg) {
                console.log(msg)
                this.$bvToast.toast(msg, {noCloseButton: true})
            },
            error(err) {
                this.$bvToast.toast(err.message, {noCloseButton: true, title: 'Error'})
                console.log(err)
            },
            limitLines(text) {
                let lines = text.split('\n')
                for (let i=0; i<lines.length; i++) {
                    let line = lines[i]
                    if (line.length > 100)
                        lines[i] = line.substring(0, 99) + '...'
                }
                return lines.join('\n')
            }
        },
        computed: {
            requestHeader() {
                return this.selectedEvent.tasks[this.selectedTask].requestHeader
            },
            requestBody() {
                return this.limitLines(this.selectedEvent.tasks[this.selectedTask].requestBody)
            },
            responseHeader() {
                return this.selectedEvent.tasks[this.selectedTask].responseHeader
            },
            responseBody() {
                return this.limitLines(this.selectedEvent.tasks[this.selectedTask].responseBody)
            },
            eventSummary() {
                const index = this.$store.state.base.currentEventIndex
                return this.$store.state.base.eventSummaries[index]
            },
        },
        created() {
            this.index = this.findEventInStore()
        },
        watch: {
            '$route': 'updateIndex',
            'this.$store.state.base.currentEventIndex': 'loadEvent',
        },
        props: [
            'eventId', 'sessionId', 'channelId'
        ],
        components: {
            LogNav,
        },
        name: "LogItem"
    }
</script>

<style scoped>
    .event-details {
        text-align: left;
        overflow: scroll;
        height: 400px;
        border: 1px solid black;
    }
    .event-description {
        font-weight: bold;
        text-align: left;
    }
    .request-response {
        position: relative;
        left: 60px;
        text-align: left;
    }
</style>
