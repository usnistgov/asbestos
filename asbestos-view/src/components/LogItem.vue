<template>
    <div>
        <log-nav :index="index" :sessionId="sessionId" :channelId="channelId"></log-nav>

        <span class="selectable"
             v-for="(reqres, i) in requestOrResponse"
             :key="'REQ' + i"
             :@click="chooseRequest(requestOrResponse[i])">
            {{ reqres }}
            <div class="divider"></div>
        </span>
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
                index: 0,
                requestOrResponse: [ "Request", "Response" ],
                selectedEvent: null,
                selectedTask: 0,
                displayRequest: true,
            }
        },
        methods: {

            chooseRequest(type) {
                this.displayRequest = type === 'Request'
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
        },
        computed: {
            requestHeader() {
                return this.selectedEvent.tasks[this.selectedTask].requestHeader
            },
            requestBody() {
                return this.selectedEvent.tasks[this.selectedTask].requestBody
            },
            responseHeader() {
                return this.selectedEvent.tasks[this.selectedTask].responseHeader
            },
            responseBody() {
                return this.selectedEvent.tasks[this.selectedTask].responseBody
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
    }
</style>
