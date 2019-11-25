<template>
    <div>
        <log-nav v-if="!noNav" :index="index" :sessionId="sessionId" :channelId="channelId"> </log-nav>

        <div v-if="eventSummary" class="event-description">
            {{ eventAsDate(eventSummary.eventName) }} - {{ eventSummary.verb}} {{ eventSummary.resourceType }} - {{ eventSummary.status ? 'Ok' : 'Error' }}
        </div>
        <div class="request-response">
            <div v-if="selectedEvent">
                <span v-for="(task, taski) in tasks" :key="taski">
                    <span v-bind:class="{ selected: taski === selectedTask, selectable: taski !== selectedTask }" @click="selectTask(taski)">
                        {{ taskLabel(taski) }}
                        <span class="divider"> </span>
                    </span>
                </span>
            </div>
            <div v-else>
                No Tasks
            </div>
           <span v-bind:class="{ selected: displayRequest === true, selectable: displayRequest === false }"
              @click="displayRequest = true">
                Request
           </span>
            <div class="divider"></div>
            <span v-bind:class="{ selected: displayRequest === false, selectable: displayRequest === true }"
                  @click="displayRequest = false">
                Response
            </span>
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
                <pre>{{responseBody}}</pre>
            </div>
        </div>
    </div>
</template>

<script>
    import LogNav from "./LogNav"
    import {LOG} from '../../common/http-common'
    import eventMixin from '../../mixins/eventMixin'
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'

    export default {
        data() {
            return {
                selectedEvent: null,
                selectedTask: 0,
                displayRequest: true,
            }
        },
        methods: {
            taskLabel(i) {
                if (i === 0)
                    return 'From Client'
                if (i === 1)
                    return 'To Server'
                return i
            },
            selectTask(i) {
                this.selectedTask = i
            },
            selectedEventName() {
                return this.selectedEvent === null ? null : this.selectedEvent.eventName
            },
            getEvent() {
                this.loadEvent()
                return this.selectedEvent
            },
            loadEvent() {
                if (!this.$store.state.log.eventSummaries)
                    return
                const summary = this.$store.state.log.eventSummaries[this.index]
                if (!summary)
                    return
                // don't reload if it is already the selected event
                const selectedEventName = summary.eventName === this.selectedEventName() ? null: summary.eventName
                if (selectedEventName !== null) {
                    this.selectedEvent = null
                    this.selectedTask = 0
                    LOG.get(`${this.sessionId}/${this.channelId}/${summary.resourceType}/${summary.eventName}`)
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
            limitLines(text) {
                return text
            },
            translateNewLines(text) {
                return text.replace(/\n/g, '<br />')
            },
            removeFormatting(msg) {
                  return msg.replace(/&lt;/g, '<').replace(/&#xa;/g, '\n').replace(/&#x9;/g, '\t')
            },
            async loadEventSummaries() {
                await this.$store.dispatch('loadEventSummaries', {session: this.sessionId, channel: this.channelId})
            },
        },
        computed: {
            index() {  // of eventId
                return (this.$store.state.log.eventSummaries)
                    ? this.$store.state.log.eventSummaries.findIndex(summary => this.eventId === summary.eventName)
                    : null
            },
            tasks() {
                return this.selectedEvent && this.selectedEvent.tasks !== undefined ? this.selectedEvent.tasks : []
            },
            taskCount() {
                return this.selectedEvent && this.selectedEvent.tasks ? this.selectedEvent.tasks.length : 0
            },
            requestHeader() {
                return this.selectedEvent.tasks[this.selectedTask].requestHeader
            },
            requestBody() {
                return this.removeFormatting(this.limitLines(this.selectedEvent.tasks[this.selectedTask].requestBody))
            },
            responseHeader() {
                return this.selectedEvent.tasks[this.selectedTask].responseHeader
            },
            responseBody() {
                return this.removeFormatting(this.limitLines(this.selectedEvent.tasks[this.selectedTask].responseBody))
            },
            eventSummary() {
                if (!this.$store.state.log.eventSummaries)
                    return null
                return (this.index > -1)
                    ? this.$store.state.log.eventSummaries[this.index]
                    : null
            },
        },
        created() {
            this.loadEventSummaries()
        },
        watch: {
            eventId() {
                if (this.noNav)
                    return
                this.loadEvent()
            },
        },
        props: [
            'eventId', 'sessionId', 'channelId', 'noNav',
        ],
        mixins: [eventMixin, errorHandlerMixin],
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
    .selected {
        font-weight: bold;
        cursor: pointer;
        text-decoration: underline;
    }
</style>
