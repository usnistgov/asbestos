<template>
    <div>
        <log-nav v-if="!noNav" :index="index" :sessionId="sessionId" :channelId="channelId"> </log-nav>

        <div class="boxed">
            <!-- Event Header -->
            <div v-if="eventSummary" class="event-description">
                {{ eventAsDate(eventSummary.eventName) }} - {{ eventSummary.verb}} {{ eventSummary.resourceType }} - {{ eventSummary.status ? 'Ok' : 'Error' }}
                <span class="client-server-position">
                <span class="bolded">Client:</span>  {{clientIP}}
                <span class="bolded">Server:</span>  {{channelId}}
            </span>
            </div>

            <div class="request-response">
                <div class="divider"></div>

                <span class="link-position solid-boxed pointer-cursor" @click.stop.prevent="copyToClipboard">Copy Event Link</span>
                <input type="hidden" id="the-link" :value="eventLink">

                <!-- From Client To Server -->
                <div v-if="selectedEvent">
                    Raw Event:
                    <span v-for="(task, taski) in tasks" :key="taski">
                        <span v-bind:class="[{ selected: taski === selectedTask, selectable: taski !== selectedTask }, 'cursor-pointer']" @click="selectTask(taski)">
                            {{ taskLabel(taski) }}
                            <span class="divider"> </span>
                        </span>
                    </span>

                    <a href="https://github.com/usnistgov/asbestos/wiki/Connectathon-FAQ#inspector" target="_blank">
                        <img src="../../assets/info.png">
                    </a>

                    <!-- Request/Response line -->
                    <div>
                        <span class="divider80"></span>
                        <span v-bind:class="{
                            selected: displayRequest,
                            'not-selected': !displayRequest
                         }"
                          @click="displayRequest = true; displayResponse = false; displayInspector = false; displayValidations = false">
                            Request
                        </span>

                        <div class="divider"></div>
                        <span v-bind:class="{
                            selected: displayResponse,
                            'not-selected': !displayResponse
                            }"
                            @click="displayRequest = false; displayResponse = true; displayInspector = false; displayValidations = false">
                            Response
                        </span>
                    </div>

                </div>
                <div v-else>
                    <div class="not-selected" @click="selectedEvent = true; displayRequest = true; displayInspector = false;"> Enable display of raw event</div>
                </div>


                <!-- Inspect Validations -->
                <div class="vdivider"></div>
                <div class="vdivider"></div>
                <div class="vdivider"></div>
            </div>

            <div class="request-response">
                <div>
                    Content:
                    <span v-bind:class="{
                        selected: inspectRequest && requestEnabled,
                        'not-selected': !(inspectRequest && requestEnabled)
                        }"
                      @click="displayRequest = false; displayResponse = false; displayInspector = true; inspectType = 'request'; displayValidations = false">
                        Inspect Request
                    </span>
                    <div class="divider"></div>
                    <span v-bind:class="{
                        selected: inspectResponse && responseEnabled,
                        'not-selected': !(inspectResponse && responseEnabled)
                        }"
                          @click="displayRequest = false; displayResponse = false; displayInspector = true; inspectType = 'response'; displayValidations = false">
                    Inspect Response/Server
                </span>
                    <div class="divider"></div>
                    <span v-bind:class="{
                    selected: displayValidations,
                    'not-selected': !displayValidations
                    }" @click="displayRequest = false; displayResponse = false; displayInspector = false; displayValidations = true">
                    PDB Validations
                </span>
                </div>
            </div>
            <br />
        </div>

        <!-- basic event display choices -->
            <div v-if="!displayInspector && !displayValidations && getEvent()">
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

            <!-- extended displays -->
            <div v-if="inspectRequest" class="request-response">
                <log-analysis-report
                        :session-id="sessionId"
                        :channel-id="channelId"
                        :event-id="eventId"
                        :request-or-response="'request'"
                        :no-inspect-label="true"></log-analysis-report>
            </div>
            <div v-if="inspectResponse" class="request-response">
                <log-analysis-report
                        :session-id="sessionId"
                        :channel-id="channelId"
                        :event-id="eventId"
                        :request-or-response="'response'"
                        :no-inspect-label="true"></log-analysis-report>
            </div>
            <div v-if="displayValidations" class="request-response">
                <eval-details
                        :session-id="sessionId"
                        :channel-id="channelId"
                        :event-id="eventId"
                        :test-id="'bundle_eval'"
                        :test-collection="'Internal'"
                        :run-eval="true"
                        :no-inspect-label="true"></eval-details>
            </div>
    </div>
</template>

<script>
    import LogNav from "./LogNav";
    import LogAnalysisReport from "./LogAnalysisReport";
    import {LOG} from '../../common/http-common';
    import eventMixin from '../../mixins/eventMixin';
    import errorHandlerMixin from '../../mixins/errorHandlerMixin';
    import EvalDetails from "../testRunner/EvalDetails";

    export default {
        data() {
            return {
                selectedEvent: false,
                selectedTask: 0,
                displayRequest: true,
                displayResponse: false,
                displayInspector: false,
                displayValidations: false,
                inspectType: 'request',
                allEnabled: false,
            }
        },
        methods: {
            copyToClipboard() {
                let linkToCopy = document.querySelector('#the-link')
                linkToCopy.setAttribute('type', 'text')
                linkToCopy.select()

                try {
                    document.execCommand('copy')
                    this.msg('Copied')
                } catch (error) {
                    this.msg('Cannot copy')
                }


                /* unselect the range */
                linkToCopy.setAttribute('type', 'hidden')
                window.getSelection().removeAllRanges()
            },
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
                // don't load if it is already the selected event
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
            requestEnabled() {
                if (this.allEnabled) return true;
                if (!this.reqresp)
                    return false;
                return this.reqresp === 'req';
            },
            responseEnabled() {
                if (this.allEnabled) return true;
                if (!this.reqresp)
                    return false;
                return this.reqresp === 'resp';
            },
            inspectRequest() {
                return this.displayInspector && this.inspectType === 'request'
            },
            inspectResponse() {
                return this.displayInspector && this.inspectType === 'response'
            },
            clientIP() {
                const idx = this.index
                return idx ? this.$store.state.log.eventSummaries[idx].ipAddr : null
            },
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
            eventLink() {
                return window.location.href
            },
        },
        created() {
            this.loadEventSummaries()
            if (this.requestEnabled && !this.responseEnabled) {
                this.displayInspector = true;
                this.inspectType = 'request';
            }
            if (!this.requestEnabled && this.responseEnabled) {
                this.displayInspector = true;
                this.inspectType = 'response';
            }
        },
        watch: {
            eventId() {
                if (this.noNav)
                    return
                this.loadEvent()
            },
        },
        props: [
            'eventId', 'sessionId', 'channelId', 'noNav', 'reqresp',
        ],
        mixins: [eventMixin, errorHandlerMixin],
        components: {
            LogNav,
            LogAnalysisReport,
            EvalDetails
        },
        name: "InspectEvent"
    }
</script>

<style scoped>
    .boxed {
        border: 1px solid rgba(0, 0, 0, 0.8);
        position: relative;
        /*left: -60px;*/
    }
    .bolded {
        font-weight: bold;
    }
    .client-server-position {
        font-weight: normal;
        position: absolute;
        left: 600px;
    }
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
    .right {
        text-align: right;
    }
    .link-position {
        position: absolute;
        left: 350px;
    }
    .not-selected {
        cursor: pointer;
        text-decoration: underline;
    }
</style>
