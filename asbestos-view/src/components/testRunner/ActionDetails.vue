<template>
    <div>
<!--        Report: {{ report }}-->
        <div v-if="script">
            <div v-bind:class="{'not-run': isNotRun, pass : isPass, error: isError, fail: isFail}"  @click="toggleMessageDisplay()">
                <span v-if="this.script.operation" class="name selectable">
                    {{ this.operationType(this.script.operation) }}
                </span>
                <span v-else>
                    <span class="selectable">assert: </span>
                </span>
                <span class="selectable">
                    {{ description }}
                </span>
            </div>
        </div>
        <div v-else>
            <!--
                Used to report general errors within the tool
            -->
            <div v-if="report[0]">
                {{ translateNL(report[0].assert.message)}}
            </div>
        </div>

        <div v-if="displayMessage">

            <div v-if="message.indexOf('#') === -1">
                <ul>
                    <div v-for="(line, linei) in translateNL(message)" :key="'msgDisp' + linei">
                        <li>
                            {{ line }}
                        </li>
                    </div>
                </ul>
            </div>
            <div v-else>
                No Evaluation
            </div>

            <div v-if="script.operation">
                <span v-if="eventId" class="selectable" @click="toggleEventDisplayed()">Inspect</span>
                <span v-if="eventDisplayed && eventId">
                    <img src="../../assets/arrow-down.png" @click="toggleEventDisplayed()">
                    <log-item
                            :sessionId="$store.state.base.session"
                            :channelId="$store.state.base.channelId"
                            :eventId="eventId"
                            :noNav="true">
                    </log-item>
                </span>
                <span v-else>
                    <span v-if="eventId">
                        <img src="../../assets/arrow-right.png" @click="toggleEventDisplayed()">
                    </span>
                </span>
            </div>

            <div>
                <span class="selectable" @click="toggleScriptDisplayed()">Test Script/Report</span>
                <span v-if="displayScript">
                    <img src="../../assets/arrow-down.png" @click="toggleScriptDisplayed()">
                    <script-display
                            :script="script"
                            :report="report">
                    </script-display>
                </span>
                <span v-else>
                    <img src="../../assets/arrow-right.png" @click="toggleScriptDisplayed()">
                </span>
            </div>
            <div>
                <span class="selectable" @click="toggleDetailsDisplayed()">Details</span>
                <span v-if="displayDetails">
                    <img src="../../assets/arrow-down.png" @click="toggleDetailsDisplayed()">
                   <vue-markdown>{{message}}</vue-markdown>
                </span>
                <span v-else>
                    <img src="../../assets/arrow-right.png" @click="toggleDetailsDisplayed()">
                </span>
            </div>
        </div>
    </div>
</template>

<script>
    import LogItem from "../logViewer/LogItem"
    import ScriptDisplay from "./ScriptDisplay"
    import VueMarkdown from 'vue-markdown'
    export default {
        data() {
            return {
                // message: null,
                displayMessage: false,
                displayScript: false,
                displayDetails: false,
                status: [],   // testName => undefined, 'pass', 'fail', 'error'
                eventLogUrl: null,
                eventDisplayed: false,
            }
        },
        methods: {
            translateNL(string) {
                if (!string)
                    return string
                return string.replace(/\t/g, "  ").split('\n')
            },
            first(array) {
                return array[0]
            },
            rest(array) {
                return array.slice(1)
            },
            toggleEventDisplayed() {
                this.eventDisplayed = !this.eventDisplayed
            },
            toggleScriptDisplayed() {
                this.displayScript = !this.displayScript
            },
            toggleDetailsDisplayed() {
                this.displayDetails = !this.displayDetails
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
        },
        computed: {
            message() {
                if (!this.report)
                    return null
                return this.report.assert
                    ? this.report.assert.message
                    : this.report.operation.message
            },
            detail() {
                if (!this.report)
                    return null
                return this.report.assert
                    ? this.report.assert.detail
                    : this.report.operation.detail
            },
            eventId() {
                const logUrl = this.detail
                if (!logUrl)
                    return null
                const parts = logUrl.split('/')
                return parts[parts.length-1]
            },
            isPass() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                if (!part) return false
                return part.result === 'pass'
            },
            isError() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                if (!part) return false
                return part.result === 'error'
            },
            isFail() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                if (!part) return false
                return part.result === 'fail'
            },
            isNotRun() {
                if (!this.report) return true
                const part = this.report.operation ? this.report.operation : this.report.assert
                if (!part) return true
                return part.result === 'skip'
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
            ScriptDisplay,
            LogItem,
            VueMarkdown
        },
        name: "ActionDetails"
    }
</script>

<style scoped>
    .assert {
        text-indent: 50px;
    }
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
        background-color: cornflowerblue;
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
