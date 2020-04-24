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

            <span v-if="displayOpen">
                <img src="../../assets/arrow-down.png">
            </span>
            <span v-else>
                <img src="../../assets/arrow-right.png"/>
            </span>

            <span class="has-cursor">
                <span v-if="label">{{label}}: </span>
                <span v-else-if="isConditional" class="bold">If: </span>
<!--                <span v-else class="bold">Test: </span>-->
                {{ description }}
            </span>
        </span>

        <div v-if="displayOpen &&  isConditional" class="conditional-margins">
            <div>  <!-- enter contained test script with conditional test -->
                <script-details-contained
                        :conditionScript="scriptConditional"
                        :conditionReport="reportConditional"
                > </script-details-contained>
            </div>
        </div>

        <!--
            (!isConditional && displayOpen) ==> default closed for most things
            isConditional ==> then is displayed by default
        -->
        <ul v-if="isConditional || (!isConditional && displayOpen)" class="noListStyle">
            <li v-for="(action, actioni) in script.action"
                v-bind:class="{
                    'action-margins': true,
                    'breakpoint-indicator': isBreakpoint(actioni),
                }"
                 :key="'Action' + actioni">
                <!--
                    If attempt to call test component fails (component not called)
                    No extension/import will be shown in report
                -->
                <div v-if="scriptContainsImport(action)">
                    <component-script
                        :action-script="action"
                        :action-report="report && report.action ? report.action[actioni] : null"> </component-script>
<!--                <div v-if="setComponentName(action, report && report.action ? report.action[actioni] : null)">-->
<!--                    <div v-for="(caction, cactioni) in componentScriptActions"-->
<!--                         :key="'CAction' + cactioni">   &lt;!&ndash;   class="action-margins"   &ndash;&gt;-->
<!--                        <action-details-->
<!--                                :script="caction"-->
<!--                                :report="componentReportActions ? componentReportActions[cactioni] : null"-->
<!--                                :debug-title="debugTitle(actioni)"-->
<!--                                @onStatusMouseOver="hoverActionIndex = actioni"-->
<!--                                @onStatusMouseLeave="hoverActionIndex = -1"-->
<!--                                @onStatusClick="toggleBreakpointIndex(actioni)"></action-details>-->
<!--                    </div>-->
<!--                </div>-->
                </div>
                <div v-else class="has-cursor">
                    <action-details
                        :script="action"
                        :report="report && report.action ? report.action[actioni] : null"
                        :debug-title="debugTitle(actioni)"
                        @onStatusMouseOver="hoverActionIndex = actioni"
                        @onStatusMouseLeave="hoverActionIndex = -1"
                        @onStatusClick="toggleBreakpointIndex(actioni)"
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
   // import importMixin from "../../mixins/importMixin";
    import TestStatusEventWrapper from "./TestStatusEventWrapper";
    import ComponentScript from "./ComponentScript";

    export default {
        data() {
            return {
                displayOpen: false,
                hoverActionIndex: -1,
                breakpointIndex: [],  // sunil - this is present here and in ComponentScript.vue - should be in store
            }
        },
        methods: {
            reportContainsImport(action, actioni) {
                if (!this.report.action) return false
                const reportAction = this.report.action[actioni]
                if (!reportAction) return false
                if (!reportAction.operation) return false
                if (!reportAction.operation.modifierExtension) return false
            },
            scriptContainsImport(action) {
                if (!action.operation) return false;
                if (!action.operation.modifierExtension) return false;
                let hasImport = false;
                action.operation.modifierExtension.forEach(extension => {
                    if (extension.url === 'https://github.com/usnistgov/asbestos/wiki/TestScript-Import')
                        hasImport = true
                })
                return hasImport
            },
            toggleDisplay() {
                this.displayOpen = !this.displayOpen
            },
            // sunil - these  methods are present here and in ComponentScript.vue.  Should be store getters so they can be shared
            toggleBreakpointIndex(actionIndex) {
                if (this.breakpointIndex[actionIndex]) {
                    this.hoverActionIndex = -1
                }
                this.breakpointIndex[actionIndex] = ! this.breakpointIndex[actionIndex]
                if (this.breakpointIndex[actionIndex]) {
                    this.hoverActionIndex = actionIndex
                    // console.log("calling dispatch" + this.testScriptIndex + " breakpointIndex: " + this.testIndex + "." + actionIndex)
                    this.$store.dispatch('addBreakpoint', {testScriptIndex: this.testScriptIndex, breakpointIndex: this.testIndex + "." + actionIndex})
                } else {
                   // remove breakpoint
                    this.$store.dispatch('removeBreakpoint', {testScriptIndex: this.testScriptIndex, breakpointIndex: this.testIndex + "." + actionIndex})
                }
            },
            debugTitle(idx) {
                if (! this.breakpointIndex[idx]) {
                    return "Set breakpoint"
                } else {
                    return "Remove breakpoint"
                }
            },
            isBreakpoint(actionIdx) {
                return Boolean(this.breakpointIndex[actionIdx]) || ! this.breakpointIndex[actionIdx] && this.hoverActionIndex === actionIdx
            }
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
            'testScriptIndex', 'testIndex',   // used by debugger
        ],
        components: {
            ActionDetails,
            ScriptDetailsContained,
            TestStatusEventWrapper,
            ComponentScript,
        },
        mixins: [
            colorizeTestReports,
        //    importMixin
        ],
        name: "TestDetails"
    }
</script>

<style scoped>

</style>
