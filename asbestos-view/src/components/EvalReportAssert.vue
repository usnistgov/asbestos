<template>
    <div>
<!--        assert pass/fail-->
        <div v-bind:class="[isAssertPass ? passClass : failClass, '']"  @click="toggleAssertMessageDisplay()">
            <span class="name selectable">assert: </span>
            <span>{{ assertionDescription }}</span>
        </div>

<!--        events listed par assert-->
        <div v-if="displayAssertMessage" class="event-part">
            <div v-for="(eventId, eventi) in eventIds" v-bind:class="[isEventPass(eventId) ? passClass : failClass, '']"
              :key="'Disp' + eventi">
                <div>
                    <eval-report-event
                            :event-id="eventId"
                            :test-report="eventResult[eventId]"
                            :action-index="actionIndex"></eval-report-event>
                </div>
            </div>
<!--            <pre>{{ message }}</pre>-->
        </div>
    </div>
</template>

<script>
    import EvalReportEvent from './EvalReportEvent'

    export default {
        data() {
            return {
                displayAssertMessage: false,
                status: [],   // testName => undefined, 'pass', 'fail', 'error'
                passClass: 'pass',
                failClass: 'fail',
            }
        },
        methods: {
            toggleAssertMessageDisplay() {  // display of the message returned in the assert
                this.displayAssertMessage = !this.displayAssertMessage
            },
            assertReport(eventId) {
                if (!this.eventResult)
                    return null;
                const testReport = this.eventResult[eventId]
                return testReport.test[this.testIndex].action[this.actionIndex].assert
            },
            assertScript() {
                return this.testScript.test[this.testIndex].action[this.actionIndex].assert
            },
            isEventPass(eventId) {
                return this.eventResult[eventId].result === 'pass'
            },
            eventDetail(eventId) {
                return this.eventResult[eventId].result
            },
        },
        computed: {
            assertionDescription() {
                return this.assertScript().description
            },
            message() {
                if (!this.testReport)
                    return null
                return this.testReport.test[this.testIndex].action[this.actionIndex].assert.message
            },
            isAssertPass() {  // scan all events
                if (!this.eventResult)
                    return false
                let pass = false
                Object.getOwnPropertyNames(this.eventResult).forEach(eventId => {
                    if (this.eventResult[eventId].result === 'pass')
                        pass = true
                })
                return pass
            },
            label() {
                return this.assertScript().label
            },
            eventIds() {
                if (!this.eventResult) {
                    console.log('no event ids')
                    return null;
                }
                console.log(`eventIds: ${Object.getOwnPropertyNames(this.eventResult)}`)
                 return Object.getOwnPropertyNames(this.eventResult)
            },
            eventResult() {
                console.log(`eventResult for evalId ${this.evalId}`)
                console.log(`eventResult => ${this.$store.state.testRunner.clientTestResult[this.evalId]}`)
                console.log(`evalIds are ${Object.getOwnPropertyNames(this.$store.state.testRunner.clientTestResult)}`)
                return this.$store.state.testRunner.clientTestResult[this.evalId]
            },
        },
        created() {

        },
        mounted() {

        },
        watch: {
        },
        props: [
            'testScript',     // full TestScript
            'testReport',     // full TestReport  ???
            'testIndex',  // index into TestReport.test
            'actionIndex',  // index into TestReport.test[testIndex].action[actionIndex]
            'evalId',
           // 'eventResult',    // array: eventId -> full TestReport (must index into actions with scriptActionIndex)
        ],
        components: {
            EvalReportEvent,
        },
        name: "EvalReportAssert"
    }
</script>

<style scoped>
    .name {
        font-weight: bold;
    }
    .event-part {
        margin-left: 25px;
        margin-right: 25px;
    }
    .plain {

    }
</style>
