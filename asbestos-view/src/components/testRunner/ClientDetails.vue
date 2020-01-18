<template>
    <div v-if="testScript">
        <div class="instruction">
            {{ testScript.description }}
        </div>
        <div v-if="eventIds === null">
            No messages present on this channel
        </div>
        <div v-else>
            <div v-for="(eventId, eventi) in eventIds"
                :key="'Disp' + eventi">
                <div>
                    <div  @click.self="selectEvent(eventId)" v-bind:class="[isEventPass(eventId) ? passClass : failClass, 'event-part']">
                        Message: {{ eventId }} - {{ eventDetail(eventId) }}
                    </div>
                    <div v-if="selected === eventId">
                        <router-view></router-view>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>

    export default {
        data() {
            return {
                passClass: 'pass',
                failClass: 'fail',
            }
        },
        methods: {
            selectEvent(name) {
                if (this.selected === name)  { // unselect
                    this.$store.commit('setCurrentEvent', null)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${this.testId}`
                    this.$router.push(route)
                } else {
                    this.$store.commit('setCurrentEvent', name)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${this.testId}/event/${name}`
                    this.$router.push(route)
                }
            },
            eventDetail(eventId) {
                if (this.logSummariesNeedLoading || this.logSummariesNeedLoading2) {
                    //console.log(`calling loadEventSummaries`)
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
            isEventPass(eventId) {
                return this.eventResult[eventId].result === 'pass'
            },
            selectCurrent() {
                this.selectEvent(this.selected)
            },
            loadTest() {
                this.$store.dispatch('loadTestScript', { testCollection: this.testCollection, testId: this.testId })
            },
        },
        computed: {
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
            this.loadTest()
        },
        watch: {
            'testId': 'loadTest'
        },
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId'
        ],
        components: {

        },
        name: "ClientDetails"
    }
</script>

<style scoped>
    .event-part {
        margin-left: 15px;
        margin-right: 15px;
        cursor: pointer;
        text-decoration: underline;
    }
</style>
