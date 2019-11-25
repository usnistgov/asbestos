<template>
    <div>
        <div>
            <span class="tool-title">Logs</span>
            <span class="divider"></span>
            <img id="reload" class="selectable" @click="loadEventSummaries()" src="../assets/reload.png"/>
        </div>

        <!--  selectable resource type list -->
        <span v-for="(type, i) in resourceTypes"
              v-bind:key="i"
              v-bind:class="{'active':(type === currentType)}">
            <span class="selectable"
                  @click="currentType = type">{{ type }}</span>
            <span class="divider"></span>
        </span>
        <div class="vdivider"></div>
        <div class="vdivider"></div>
        <div class="vdivider"></div>

        <!--  list summaries for currentType      -->
        <div v-for="(eventSummary, i) in eventSummariesByType"
             :key="currentType + i">
            <div >
                <div class="summary-label boxed has-cursor"
                     @click="selectSummary(eventSummary)">
                    {{ eventAsDate(eventSummary.eventName) }} - {{ eventSummary.verb}} {{ eventSummary.resourceType }} - {{ eventSummary.status ? 'Ok' : 'Error' }}
                </div>

                <!-- list tasks if event summary is selected-->
                <div v-if="selectedEventName === eventSummary.eventName && selectedEvent">
                    <span class="selectable"
                          v-for="(task, i) in selectedEvent.tasks"
                          :key="'A' + currentType + i"
                          @click="chooseTask(i)"
                          v-bind:class="{'active':(task.index === selectedTask)}">
                        Task{{ i }}
                    </span>
                </div>
                <div v-if="selectedEvent">
                    <div v-if="selectedEventName === eventSummary.eventName && selectedEvent">
                        <span class="selectable"
                        v-for="(reqres, i) in requestOrResponse"
                        :key="'REQ' + selectedEventName + i"
                        :@click="chooseRequest(requestOrResponse[i])">
                            {{ reqres }}
                        </span>
                        <div v-if="displayRequest" class="event-details">
                            <pre>{{ selectedEvent.tasks[selectedTask].requestHeader }}
                            </pre>
                            <pre>{{ selectedEvent.tasks[selectedTask].requestBody }}</pre>
                        </div>
                        <div v-if="!displayRequest" class="event-details">
                            <pre>{{ selectedEvent.tasks[selectedTask].responseHeader }}
                            </pre>
                            <pre>{{ selectedEvent.tasks[selectedTask].responseBody }}</pre>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { TooltipPlugin, ToastPlugin } from 'bootstrap-vue'
    Vue.use(TooltipPlugin)
    import {LOG} from '../common/http-common'
    Vue.use(ToastPlugin)

    export default {
        data() {
            return {
                monthNames: [ 'Jan', 'Feb', 'Mar', 'April', 'May', 'June', 'July', 'Aug', 'Sept', 'Oct', 'Nov', 'Dec'],
                resourceTypes: [],
                eventSummaries: [], // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false }
                currentType: null,
                eventSummariesByType: [],
                selectedEvent: null,
                selectedEventName: null,
                selectedTask: 0,
                displayRequest: true,
                requestOrResponse: [ "Request", "Response" ]
            }
        },
        methods: {
            chooseRequest(type) {
                this.displayRequest = type === 'Request'
            },
            updateEventSummariesByType() {  // called by watcher when currentType is updated
                const type = this.currentType
                const summaries = this.eventSummaries
                if (type === 'All') {
                    this.eventSummariesByType = summaries.sort((a, b) => a.eventName > b.eventName ? -1 : 1)
                } else {
                    this.eventSummariesByType =  summaries.filter(item => {
                        return item.resourceType === type
                    }).sort((a, b) => a.eventName > b.eventName ? -1 : 1)
                }
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
            chooseTask(task) {
                this.selectedTask = task
            },
            selectSummary(summary) {
                // don't reload if it is already the selected event
                this.selectedEventName = summary.eventName === this.selectedEventName ? null: summary.eventName
                if (this.selectedEventName !== null) {
                    this.selectedEvent = null
                    this.selectedTask = 0
                    LOG.get(`${this.sessionId}/${this.channelId}/${summary.resourceType}/${summary.eventName}`)
                        .then(response => {
                            try {
                                this.selectedEvent = response.data
                            } catch (error) {
                                this.error(error)
                                return
                            }
                        })
                        .catch(error => {
                            console.log('error')
                            this.error(error)
                        })
                }
            },
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

        },
        created() {
            this.loadEventSummaries()
        },
        mounted() {
        },
        watch: {
            'currentType': 'updateEventSummariesByType'
        },
        props: [
            'sessionId', 'channelId'
        ],
        name: "LogsView"
    }
</script>

<style scoped>
    .summary-label {
        background-color: lightGray;
        text-align: left;
    }
    .active {
        background-color: lightGray;
    }
    .header-area {
        display: grid;
    }
    .event-type-header  {
        padding: 2px;
        background-color: lightGray;
    }
    pre {
        font-size: .7rem;
        margin: 0;
    }
    .event-details {
        text-align: left;
    }
</style>
