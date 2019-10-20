<template>
    <div>
        <div v-bind:class="{'not-run': isNotRun, pass : isPass, fail: isError}"  @click="toggleMessageDisplay()">
            <span v-if="this.script.operation" class="name selectable">
                {{ this.operationType(this.script.operation) }}
            </span>
            <span v-else>
                <span class="name selectable">assert: </span>
            </span>
            <span class="selectable">
                {{ description }}
            </span>
        </div>

        <div v-if="displayMessage">
            <div v-for="(line, linei) in translateNL(message)" :key="msgDisp + linei">
                {{ line }}
            </div>
            <div>
                <span class="selectable" @click="toggleLogDisplayed()">Log</span>
                <span v-if="logDisplayed">
                    <img src="../../assets/arrow-down.png" @click="toggleLogDisplayed()">
                    <log-item :sessionId="$store.state.base.session" :channelId="$store.state.base.channelId" :eventId="$store.state.testRunner.currentEvent" :noNav="true"></log-item>
                </span>
                <span v-else>
                    <img src="../../assets/arrow-right.png" @click="toggleLogDisplayed()">
                </span>
            </div>
        </div>
    </div>
</template>

<script>
    import LogItem from "../logViewer/LogItem"
    export default {
        data() {
            return {
                // message: null,
                displayMessage: false,
                status: [],   // testName => undefined, 'pass', 'fail', 'error'
                eventLogUrl: null,
                logDisplayed: false,
            }
        },
        methods: {
            translateNL(string) {
                return string.split('\n')
            },
            toggleLogDisplayed() {
                this.logDisplayed = !this.logDisplayed
            },
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
            description() {
                return this.script.operation ? this.script.operation.description : this.script.assert.description
            },
        },
        created() {

        },
        mounted() {

        },
        watch: {
            report: function(action) {
                  if (action && action.operation && action.operation.detail) {
                      this.eventLogUrl = action.operation.detail
                  }
            },
        },
        props: [
            // parts representing a single action
            'script', 'report',
        ],
        components: {
            LogItem
        },
        name: "TestReportAction"
    }
</script>

<style scoped>
    .name {
        font-weight: bold;
    }
    .pass {
        background-color: lightgreen;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .fail {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .error {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .not-run {
        background-color: lightgray;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }

</style>
