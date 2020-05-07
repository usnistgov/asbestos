<template>
    <div>
        <div v-if="script">
            <span
                    v-bind:class="{
                'not-run': isNotRun && colorful,
                'not-run-plain-detail': isNotRun && !colorful,
                'pass-plain-detail': isPass && !colorful,
                pass : isPass && colorful,
                error: isError && colorful,
                'error-plain': isError && !colorful,
                fail: isFail && colorful,
                'fail-plain-detail': isFail && !colorful,
            }"
                    @click.stop="toggleMessageDisplay()">

                <test-status-event-wrapper v-if="!statusRight"
                                           :status-on-right="statusRight"
                                           :script="script"
                                           :report="report"
                                           :debug-title="debugTitle"
                                           @onStatusMouseOver="$emit('onStatusMouseOver')"
                                           @onStatusMouseLeave="$emit('onStatusMouseLeave')"
                                           @onStatusClick="$emit('onStatusClick')"
                > </test-status-event-wrapper>

                <span v-if="displayMessage">
                    <img src="../../assets/arrow-down.png">
                </span>
                <span v-else>
                    <img src="../../assets/arrow-right.png"/>
                </span>

                <span v-if="script.operation" class="name">
                    {{ operationType(script.operation) }}
                </span>
                <span v-else>
                    <span v-if="isConditional">if: </span>
                    <span v-else>assert: </span>
                </span>
                <span>
                    {{ description }}
                </span>

                <test-status v-if="statusRight"
                             :status-on-right="statusRight"
                             :report="report"
                > </test-status>
            </span>

            <div v-if="displayMessage && assertions">
                <div v-for="(ref, refi) in references"
                    :key="'Ref' + refi" class="assert">
                    <div v-if="assertions && assertions[ref]">
                        <div v-if="assertions[ref].startsWith('http')">
                            Reference:
                            <a v-bind:href="assertions[ref]" target="_blank">
                                {{assertions[ref] }}
                            </a>
                        </div>
                        <div v-else>
                            Reference: {{ assertions[ref] }}
                        </div>
                    </div>
                </div>
            </div>

        </div>
        <div>
            <!--
             Used to report general errors within the tool
            -->

            <div v-if=" report && report[0]">
                {{ translateNL(report[0].assert.message)}}
            </div>
        </div>

        <div v-if="displayMessage">

            <div v-if="message && message.indexOf('#') === -1">
                <ul>
                    <div v-for="(line, linei) in translateNL(message)" :key="'msgDisp' + linei">
                        <li>
                        <span v-if="isError">
                            <img src="../../assets/yellow-error.png">
                        </span>
                            {{ line }}
                        </li>
                    </div>
                </ul>
            </div>
<!--            <div v-else>-->
<!--                No Evaluation-->
<!--            </div>-->

            <!--  Inspect-->
            <div v-if="script.operation">

                <span v-if="eventDisplayed && eventId">
                    <img src="../../assets/arrow-down.png" @click.stop="toggleEventDisplayed()">
                </span>
                <span v-else>
                    <span v-if="eventId">
                        <img src="../../assets/arrow-right.png" @click.stop="toggleEventDisplayed()">
                    </span>
                </span>
                <span v-if="eventId" class="selectable" @click.stop="toggleEventDisplayed()">Inspect</span>
                <span v-if="eventDisplayed && eventId">
                    <inspect-event
                        :sessionId="$store.state.base.session"
                        :channelId="$store.state.base.channelId"
                        :eventId="eventId"
                        :noNav="true">
                    </inspect-event>
                </span>
            </div>

            <!-- Test Script/Report -->
            <div>
                <span v-if="displayScript">
                    <img src="../../assets/arrow-down.png" @click.stop="toggleScriptDisplayed()">
                </span>
                <span v-else>
                    <img src="../../assets/arrow-right.png" @click.stop="toggleScriptDisplayed()">
                </span>
                <span class="selectable" @click.stop="toggleScriptDisplayed()">Test Script/Report</span>
                <span v-if="displayScript">
<!--                    <vue-markdown v-if="message">{{message}}</vue-markdown>-->
                    <hr />
                    <vue-markdown v-if="actionContext">{{actionContext}}</vue-markdown>
                    <script-display
                        :script="script"
                        :report="report">
                    </script-display>
                    <hr />
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
    import TestStatus from "./TestStatus";

    export default {
        data() {
            return {
                // message: null,
                displayMessage: false,
                displayScript: true,
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
                return operation && operation.type ? operation.type.code : null
            },
            assertionDescription() {
                return this.script.assert.description === undefined ? "" : this.script.assert.description
            },
            toggleMessageDisplay() {
                this.displayMessage = !this.displayMessage
            },
            getExtension(root, url) {
                if (!root || !root.extension) return null;
                let ext = null;
                root.extension.forEach(e => {
                    if (e.url === url)
                        ext = e;
                })
                return ext;
            },
            getExtensionValue(root, url) {
                const ext = this.getExtension(root, url);
                if (!ext) return null;
                return ext.valueString;
            },
        },
        computed: {
            isConditional() {
                if (!this.script.modifierExtension) return false
                let cond = false
                this.script.modifierExtension.forEach(ext => {
                    if (ext.url === 'https://github.com/usnistgov/asbestos/wiki/TestScript-Conditional')
                        cond = true
                })
                return cond
            },
            assertions() {
                return this.$store.state.testRunner.testAssertions
            },
            isError() {
                return this.result === 'error'
            },
            result() {
                if (!this.report)
                    return null
                return this.report.assert
                    ? this.report.assert.result
                    : this.report.operation.result

            },
            message() {
                if (!this.report)
                    return null
                return this.report.assert
                    ? this.report.assert.message
                    : this.report.operation.message
            },
            actionContext() {
                if (!this.report)
                    return null;
                return this.report.assert
                    ? this.getExtensionValue(this.report.assert, "urn:action-context")
                    : this.getExtensionValue(this.report.operation, "urn:action-context");
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
                return parts[parts.length - 1]
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
                const attr = this.descriptionAtt
                const parts = attr.split('|')
                return parts[0]
            },
            references() {
                const attr = this.descriptionAtt
                let parts = attr.split('|')
                parts.shift()
                return parts
            },
            descriptionAtt() {
                return this.script.operation ? this.script.operation.description : this.script.assert.description
            },
        },
        created() {

        },
        mounted() {

        },
        watch: {
            report: function (action) {
                if (action && action.operation && action.operation.detail) {
                    this.eventLogUrl = action.operation.detail
                }
            },
        },
        props: [
            // parts representing a single action
            'script', 'report', 'debugTitle',
        ],
        components: {
            ScriptDisplay,
            InspectEvent,
            VueMarkdown,
            TestStatus,
            TestStatusEventWrapper,
        },
        mixins: [colorizeTestReports],
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
</style>