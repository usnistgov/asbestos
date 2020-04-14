<template>
    <div class="inlineDiv">
        <div class="pre-test-gap"></div>
        <span v-bind:class="{
            pass:         isPass  && colorful,
            'pass-plain-detail': isPass  && !colorful,
            fail:         isFail  && colorful,
            'fail-plain-detail': isFail  && !colorful,
             error:       isError && colorful,
             'error-plain': isError && !colorful,
             'not-run':   isNotRun && colorful,
             'not-run-plain-detail' : isNotRun && ! colorful,
        }"  class="test-margins" @click.stop="toggleDisplay()">

            <test-status-event-wrapper v-if="!statusRight"
                         :status-on-right="statusRight"
                         :report="report"
            > </test-status-event-wrapper>

        <span v-if="display">
            <img src="../../assets/arrow-down.png">
        </span>
        <span v-else>
            <img src="../../assets/arrow-right.png"/>
        </span>

            <span>
                <span v-if="label" class="bold">{{label}}: </span>
                <span v-else class="bold">Test: </span>
                {{ description }}
            </span>
        </span>

        <test-status-event-wrapper v-if="statusRight"
                     :status-on-right="statusRight"
                     :report="report"
        > </test-status-event-wrapper>


        <div v-if="isConditional" class="conditional-margins">
            <div>
                <script-details-contained
                        :script="scriptConditional"
                        :report="reportConditional"
                > </script-details-contained>
            </div>
        </div>

        <ul v-if="display" class="noListStyle">
            <li v-for="(action, actioni) in script.action"
                v-bind:class="{
                    'action-margins': true,
                    'breakpoint-indicator': showBreakpointIndicator(testScriptIndex, testType, testIndex, actioni),
                    'breakpoint-hit-indicator': isBreakpointHit(testScriptIndex, testType, testIndex, actioni),
                }"
                 :key="'Action' + actioni">
                <div v-if="setImportComponentName(action)">
                    <div v-for="(caction, cactioni) in componentScriptActions" class="action-margins"
                         :key="'CAction' + cactioni">
                        <action-details
                                :script="caction"
                                :report="componentReportActions ? componentReportActions[cactioni] : null"
                                :debug-title="debugTitle(testScriptIndex, testType, testIndex, actioni)"
                                @onStatusMouseOver="hoverActionIndex = actioni"
                                @onStatusMouseLeave="hoverActionIndex = -1"
                                @onStatusClick="toggleBreakpointIndex(testScriptIndex, testType, testIndex, actioni)"></action-details>
                    </div>
                </div>
                <div v-else>
                    <action-details
                        :script="action"
                        :report="report && report.action ? report.action[actioni] : null"
                        :debug-title="debugTitle(testScriptIndex, testType, testIndex, actioni)"
                        @onStatusMouseOver="hoverActionIndex = actioni"
                        @onStatusMouseLeave="hoverActionIndex = -1"
                        @onStatusClick="toggleBreakpointIndex(testScriptIndex, testType, testIndex, actioni)"
                    >
                    </action-details>
                </div>
            </li>
        </ul>
    </div>
</template>

<script>
    import ActionDetails from './ActionDetails'
    import ScriptDetailsContained from "./ScriptDetailsContained";
    import colorizeTestReports from "../../mixins/colorizeTestReports";
   // import TestStatus from "./TestStatus";
    import importMixin from "../../mixins/importMixin";
    import TestStatusEventWrapper from "./TestStatusEventWrapper";

    export default {
        data() {
            return {
                display: false,
                hoverActionIndex: -1,
            }
        },
        methods: {
            toggleDisplay() {
                this.display = !this.display
            },
            toggleBreakpointIndex(testScriptIndex, testType, testIndex, actionIndex) {
                // console.log("enter toggleBreakpointIndex")
                let obj = {testScriptIndex: testScriptIndex, breakpointIndex: testType + testIndex + "." + actionIndex}
                if (! this.$store.getters.hasBreakpoint(obj)) {
                     this.hoverActionIndex = actionIndex // Restore the hoverActionIndex when toggle on the same item goes from on(#)-off(-1)-on(#)
                    // console.log("calling dispatch" + testScriptIndex + " breakpointIndex: " + testIndex + "." + actionIndex)
                    this.$store.dispatch('addBreakpoint', obj)

                } else {
                    this.hoverActionIndex = -1 // Immediately remove the debug indicator while the mouse hover is still active but without having to wait for the mouseLeave event
                   // remove breakpoint
                   //  console.log("calling removeBreakpoint dispatch" + testScriptIndex + " breakpointIndex: " + testIndex + "." + actionIndex)
                    this.$store.dispatch('removeBreakpoint', obj)
                }
            },
            debugTitle(testScriptIndex, testType, testIndex, actionIndex) {
                let obj = {testScriptIndex: testScriptIndex, breakpointIndex: testType + testIndex + "." + actionIndex}
                return this.$store.getters.getDebugTitle(obj);
            },
            showBreakpointIndicator(testScriptIndex, testType, testIndex, actionIndex) {
                let obj = {testScriptIndex: testScriptIndex, breakpointIndex: testType + testIndex + "." + actionIndex}
                return (this.$store.getters.hasBreakpoint(obj) || this.hoverActionIndex === actionIndex) && ! this.isBreakpointHit(testScriptIndex, testType, testIndex, actionIndex)
            },
            isBreakpointHit(testScriptIndex, testType, testIndex, actionIndex) {
                let obj = {testScriptIndex: testScriptIndex, breakpointIndex: testType + testIndex + "." + actionIndex}
                return this.$store.getters.isBreakpointHit(obj)
            },
        },
        computed: {
            scriptConditional() { // TestScript representing conditional
                if (!this.scriptConditionalRef) return null
                let conditional = null
                this.scriptContained.forEach(contained => {
                    if (contained.id === this.scriptConditionalRef)
                        conditional = contained
                })
                return conditional
            },
            reportConditional() { // TestReport representing conditional
                if (!this.scriptConditionalRef) return null
                let conditional = null
                if (this.reportContained) {
                    this.reportContained.forEach(contained => {
                        if (contained.id === this.scriptConditionalRef && contained.resourceType === 'TestReport')
                            conditional = contained
                    })
                }
                return conditional
            },
            isConditional() {
                return this.script.modifierExtension && this.script.modifierExtension[0].url === 'https://github.com/usnistgov/asbestos/wiki/TestScript-Conditional'
            },
            scriptConditionalRef() {
                if (!this.script.modifierExtension) return null
                const ref = this.script.modifierExtension[0].valueReference.reference  // [0] => there can be only one
                if (ref) return ref.substring(1)  // ref without preceding #
                return null
            },
            description() {
                if (!this.script) return ""
                return this.script.description
            },
        },
        created() {
        },
        props: [
            // parts representing a single test element of a TestScript
            'script', 'report',
            'scriptContained', 'reportContained', // contained section of the TestScript and TestReport
            'label',
            'testScriptIndex', 'testIndex', 'testType',
        ],
        components: {
            ActionDetails, ScriptDetailsContained, TestStatusEventWrapper
        },
        mixins: [colorizeTestReports, importMixin],
        name: "TestDetails"
    }
</script>

<style scoped>

</style>
