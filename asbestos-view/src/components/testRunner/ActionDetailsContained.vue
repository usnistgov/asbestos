<template>
    <div>
        <div v-if="script">
            <span v-bind:class="{
                'not-run-plain-detail': isNotRun,
                'pass-plain-detail' : isPass,
                'error-plain': isError,
                'fail-plain-detail': isFail}"  @click.stop="toggleMessageDisplay()">

                <test-status-event-wrapper v-if="!statusRight"
                                           :status-on-right="statusRight"
                                           :report="report"
                                           :debug-title="debugTitle"
                                           @onStatusMouseOver="$emit('onStatusMouseOver')"
                                           @onStatusMouseLeave="$emit('onStatusMouseLeave')"
                                           @onStatusClick="$emit('onStatusClick')"
                > </test-status-event-wrapper>

                <span v-if="this.script.operation" class="has-cursor">
                    {{ this.operationType(this.script.operation) }}:
                </span>
                <span v-else>
                    <span class="has-cursor">assert: </span>
                </span>
                <span class="has-cursor">
                    {{ description }}
                </span>
            </span>
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
<!--            <div v-else>-->
<!--                No Evaluation-->
<!--            </div>-->

            <div v-if="script.operation">
               <span v-if="eventDisplayed && eventId">
                    <img src="../../assets/arrow-down.png" @click="toggleEventDisplayed()">
               </span>
                <span v-else>
                    <span v-if="eventId">
                        <img src="../../assets/arrow-right.png" @click="toggleEventDisplayed()">
                    </span>
                </span>

                <span v-if="eventId" class="selectable" @click="toggleEventDisplayed()">Inspect</span>
                <span v-if="eventDisplayed && eventId">
                    <InspectEvent
                            :sessionId="$store.state.base.channel.testSession"
                            :channelId="$store.state.base.channel.channelName"
                            :eventId="eventId"
                            :noNav="true">
                    </InspectEvent>
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
    import InspectEvent from "../logViewer/InspectEvent"
    import ScriptDisplay from "./ScriptDisplay"
    import VueMarkdown from 'vue-markdown'
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import TestStatusEventWrapper from "./TestStatusEventWrapper";

    export default {
        data() {
            return {
                // message: null,
                displayMessage: false,
                displayScript: true,
                displayDetails: false,
                status: [],   // testName => undefined, 'pass', 'fail', 'error'
                eventLogUrl: null,
                eventDisplayed: true,
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
                return operation && operation.type ? operation.type.code : null
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
            startDisplayOpen: function(value) {
                this.eventDisplayed = value;
            }
        },
        props: [
            // parts representing a single action
            'script', 'report', 'startDisplayOpen',
        ],
        components: {
            ScriptDisplay,
            InspectEvent,
            VueMarkdown,
            TestStatusEventWrapper
        },
        mixins: [colorizeTestReports],
        name: "ActionDetailsContained"
    }
</script>

<style scoped>
    .assert {
        text-indent: 50px;
    }
    .name {
        font-weight: bold;
    }
</style>
