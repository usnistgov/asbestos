export default {
    data() {
        return {

        }
    },
    methods: {
        assertForEvent(evalId, assertIndex, eventId) {
            const result = this.clientResult[evalId]
            const testReport = result[eventId]
            return testReport.test[0].action[assertIndex].assert
        },
        passingEvents(evalId, assertIndex) {  // => [eventIds]
            let events = []
            const eventIds = Object.getOwnPropertyNames(this.clientResult[evalId])
            eventIds.forEach(eventId => {
                const assert = this.assertForEvent(evalId, assertIndex, eventId)
                if (assert.result === 'pass')
                    events.push(eventId)
            })
            return events
        }
    },
    computed: {
        clientResult() {
            return this.$store.state.testRunner.clientTestResult
        }
    },
}