<template>
    <div v-if="testScript" class="align-left test-margins">

        <script-status v-if="!statusRight" :status-right="statusRight" :name="testId"> </script-status>

        <span v-if="$store.state.testRunner.currentEvent === eventId">
            <img src="../../assets/arrow-down.png">
        </span>
        <span v-else>
            <img src="../../assets/arrow-right.png"/>
        </span>

        <span  @click.self="selectEvent()" class="event-part" v-bind:class="[isEventPass() ? passClass : failClass]">
            Message: {{ eventId }} - {{ eventDetail(eventId) }}
        </span>

        <script-status v-if="statusRight" :status-right="statusRight" :name="testId"> </script-status>


        <div v-if="selected === eventId">
            <router-view></router-view>
        </div>
    </div>
</template>

<script>
import colorizeTestReports from "../../mixins/colorizeTestReports";
import ScriptStatus from "./ScriptStatus";

    export default {
        data() {
            return {
                passClass: null,  // initialized in created()
                failClass: null,
            }
        },
        methods: {
            selectEvent() {
                if (this.selected === this.eventId)  { // unselect
                    this.$store.commit('setCurrentEvent', null)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${this.testId}`
                    this.$router.push(route)
                } else {
                    this.$store.commit('setCurrentEvent', this.eventId)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${this.testId}/event/${this.eventId}`
                    this.$router.push(route)
                }
            },
            eventDetail(eventId) {
                if (this.logSummariesNeedLoading || this.logSummariesNeedLoading2) {
                    console.log(`calling loadEventSummaries`)
                    this.$store.dispatch('loadEventSummaries', {session: this.sessionId, channel: this.channelId})
                    //console.log(`loadEventSummaries returned`)
                }
                if (this.$store.state.log.eventSummaries) {
                    const summary = this.$store.state.log.eventSummaries.find(it =>
                        it.eventName === eventId)
                    if (summary)
                        return `${summary.verb} ${summary.resourceType} from ${summary.ipAddr}`
                }
                return null
            },
            isEventPass() {
                return this.eventResult[this.eventId].result === 'pass'
            },
            selectCurrent() {
                this.selectEvent(this.selected)
            },
            loadTest() {
                if (!this.$store.state.testRunner.testScripts[this.testId])
                    this.$store.dispatch('loadTestScript', { testCollection: this.testCollection, testId: this.testId })
            },
        },
        computed: {
            isPass() {
                return this.eventResult[this.eventId].result === 'pass'
            },
            isFail() {
                return this.eventResult[this.eventId].result === 'fail'
            },
            logSummariesNeedLoading() {  // because of channel change
                return !this.$store.state.log.eventSummaries ||
                    this.sessionId !== this.$store.state.log.session ||
                        this.channelId !== this.$store.state.log.channel
            },
            logSummariesNeedLoading2() {  // because there are eventIds not present in summaries
                if (!this.eventIds) return false
                const lastEventId = this.eventIds[0]
                if (!this.$store.state.log.eventSummaries) return true
                const lastSummaryId = this.$store.state.log.eventSummaries[0].eventName
                return lastEventId > lastSummaryId
            },
            testScript() {
                return this.$store.state.testRunner.testScripts[this.testId]
            },
            selected() {
                return this.$store.state.testRunner.currentEvent
            },
            eventIds() {
                if (!this.eventResult) {
                    return null;
                }
                return Object.keys(this.eventResult).sort().reverse()
            },
            eventResult() {
                return this.$store.state.testRunner.clientTestResult[this.testId]
            },
        },
        created() {
            if (this.$store.state.testRunner.colorMode) {
                this.passClass = 'pass'
                this.failClass = 'fail'
            } else {
                this.passClass = 'pass-plain-detail'
                this.failClass = 'fail-plain-detail'
            }
            this.loadTest()
        },
        watch: {
            'testId': 'loadTest'
        },
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId', 'eventId'
        ],
        components: {
            ScriptStatus
        },
        mixins: [colorizeTestReports],
        name: "ClientDetails"
    }
</script>

<style scoped>
    .event-part {
        /*margin-left: 5px;*/
        /*margin-right: 15px;*/
        cursor: pointer;
        text-decoration: underline;
    }
</style>
