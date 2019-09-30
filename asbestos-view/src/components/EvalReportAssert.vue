<template>
<!--    foreach action/assert in the eval (test)-->
    <div>
        <div v-bind:class="{'not-run': isNotRun, pass : isPass, fail: isError}"  @click="toggleAssertMessageDisplay()">
            <span class="name selectable">assert: </span>
            <span>{{ this.assertionDescription() }}</span>
            <span class="selectable">
                {{ label }}
            </span>
        </div>
        <div v-if="displayAssertMessage"><pre>{{ message }}</pre></div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                displayAssertMessage: false,
                status: [],   // testName => undefined, 'pass', 'fail', 'error'
            }
        },
        methods: {
            operationType(operation) {
                return operation.type.code
            },
            assertionDescription() {
                return this.assertScript().description === undefined ? "" : this.script.assert.description
            },
            toggleAssertMessageDisplay() {  // display of the message returned in the assert
                this.displayAssertMessage = !this.displayAssertMessage
            },
            nextSpace(str, idx) {
                for (let i=idx; i<str.length; i++) {
                    if (str.charAt(i) === ' ')
                        return i
                }
                return null
            },
            breakNear(str, pos) {
                let here = 0
                while(here !== null && here < pos) {
                    here = this.nextSpace(str, here)
                }
                return here
            },
            assertReport(eventId) {
                if (!this.eventResult)
                    return null;
                const testReport = this.eventResult[eventId]
                return testReport.test[this.testIndex].action[this.actionIndex].assert
            },
            assertScript() {
                return this.testScript.test[this.testIndex].action[this.actionIndex].assert
            }
        },
        computed: {
            message() {
                if (!this.testReport)
                    return null
                return this.testReport.test[this.testIndex].action[this.actionIndex].assert.message
            },
            isPass() {
                if (!this.report) return false
                if (!this.report.assert) return false
                return this.report.assert.result !== 'error' && this.report.assert.result !== 'fail'
            },
            isError() {
                if (!this.report) return false
                if (!this.report.assert) return false
                return this.report.assert.result === 'error' || this.report.assert.result === 'fail'
            },
            isNotRun() {
                return !this.report
            },
            label() {
                return this.assertScript().label
            },
            testId() {
                return this.$store.state.testRunner.currentTest
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
            'testReport',     // full TestReport
            'testIndex',  // index into TestReport.test
            'actionIndex',  // index into TestReport.test[testIndex].action[actionIndex]
            'eventResult',    // array: eventId -> full TestReport (must index into actions with scriptActionIndex)
        ],
        name: "EvalReportAssert"
    }
</script>

<style scoped>
    .name {
        font-weight: bold;
    }
</style>
