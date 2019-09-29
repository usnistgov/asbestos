<template>
    <div>
        <div v-bind:class="{'not-run': isNotRun, pass : isPass, fail: isError}"  @click="toggleMessageDisplay()">
            <span class="name selectable">assert: </span>
            <span>{{ this.assertionDescription() }}</span>
            <span class="selectable">
                {{ label }}
            </span>
        </div>
        <div v-if="displayMessage"><pre>{{ message }}</pre></div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                // message: null,
                displayMessage: false,
                status: [],   // testName => undefined, 'pass', 'fail', 'error'
                eventResult: [],
            }
        },
        methods: {
            operationType(operation) {
                return operation.type.code
            },
            assertionDescription() {
                return this.script.assert.description === undefined ? "" : this.script.assert.description
            },
            toggleMessageDisplay() {
                this.displayMessage = !this.displayMessage
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
            importEventResult() {
                this.eventResult = this.$store.state.testRunner.clientTestResult[this.testId]
            }
        },
        computed: {
            message() {
                if (!this.report)
                    return null
                const rawMessage =  this.report.assert
                    ? this.report.assert.message
                    : this.report.operation.message
                return rawMessage
            },
            isPass() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                if (!part) return false
                return part.result !== 'error' && part.result !== 'fail'
            },
            isError() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                if (!part) return false
                return part.result === 'error' || part.result === 'fail'
            },
            isNotRun() {
                return !this.report
            },
            operationOrAssertion() {
                return this.script.operation
                    ? `${this.operationType(this.script.operation)}`
                    : `Assert: ${this.assertionDescription()}`
            },

            label() {
                return this.script.operation ? this.script.operation.label : this.script.assert.label
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
            'testId': 'importEventResult'
        },
        props: [
            // parts representing a single action
            'script', 'report',
        ],
        name: "EvalReportAction"
    }
</script>

<style scoped>
    .name {
        font-weight: bold;
    }
</style>
