<template>
    <div>
        <div>
            <span class="tool-title">Events for Channel {{ channelId }}</span>
            <span class="divider"></span>

            <img id="reload" class="selectable" @click="loadEventSummaries()" src="../../assets/reload.png"/>
            <div class="divider"></div>
        </div>

        <div>
            Source IP addr filter:
            <select v-model="selectedIP" v-bind:size="1">
                <option v-for="(coll, colli) in ipAddresses"
                        v-bind:value="coll"
                        :key="coll + colli"
                        >
                    {{ coll }}
                </option>
            </select>
        </div>
        <div class="vdivider"></div>

        <div v-for="(eventSummary, i) in eventSummaries"
             :key="eventSummary.eventName + i">
            <div v-if="selectedIP === 'all' || eventSummary.ipAddr === selectedIP">
                <div class="summary-label boxed has-cursor left"
                     @click="selectSummary(eventSummary)">
                    {{ eventAsDate(eventSummary.eventName) }} - {{ eventSummary.ipAddr }} - {{ eventSummary.verb}} {{ eventSummary.resourceType }} - {{ eventSummary.status ? 'Ok' : 'Error' }}
                </div>

            </div>
        </div>
    </div>
</template>

<script>
    import eventMixin from '../../mixins/eventMixin'
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'

    export default {
        data() {
            return {
                eventSummariesByType: [],
                selectedEventName: null,
                selectedEvent: null,
                selectedTask: 0,
                selectedIP: "all",
            }
        },
        methods: {
            selectSummary(summary) {
                //this.$store.commit('setEventSummaries', this.eventSummaries)
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/lognav/${summary.eventName}?clientIP=${summary.ipAddr}`)
            },
            loadEventSummaries() {
                this.$store.dispatch('loadEventSummaries', {session: this.sessionId, channel: this.channelId})
            }
        },
        created() {
            this.loadEventSummaries()
        },
        computed: {
            eventSummaries() {   // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false }
                return this.$store.state.log.eventSummaries
            },
            ipAddresses() {
                let addrs = this.$store.getters.ipAddresses
                addrs.push("all")
                return addrs
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
