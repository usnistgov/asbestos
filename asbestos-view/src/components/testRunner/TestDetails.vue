<template>
    <div class="inlineDiv">
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
            <debuggable-list-item
                    v-for="(action, actioni) in script.action"
                    :key="'Action' + actioni"
                    :breakpoint-index="getBreakpointIndex(testType, testIndex, actioni)"
            >
                <!--
                    If attempt to call test component fails (component not called)
                    No extension/import will be shown in report
                -->
                <div v-if="report && report.action && report.action[actioni] && reportContainsError(report.action[actioni])">
                    <action-details
                            :script="action"
                            :report="report && report.action ? report.action[actioni] : null"
                    >
                    </action-details>
                </div>
                <!-- a successful call to a module is reported here.
                    The action may fail but at least the module call was good
                    -->
                <div v-else-if="scriptContainsImport(action)">
                    <component-script
                        :action-script="action"
                        :action-report="report && report.action ? report.action[actioni] : null"> </component-script>
                </div>
                <div v-else class="has-cursor">
                    <action-details
                        :script="action"
                        :report="report && report.action ? report.action[actioni] : null"
                    >
                    </action-details>
                </div>
            </debuggable-list-item>
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
    import DebuggableListItem from "./debugger/DebuggableListItem";
    import debugTestScriptMixin from "../../mixins/debugTestScript";

    export default {
        data() {
            return {
                displayOpen: false,
            }
        },
        methods: {
            // did call to module fail (different from action in module failing)
            reportContainsError(reportAction) {
                if (!reportAction)
                    return false;
                if (reportAction.operation && reportAction.operation.result && reportAction.operation.result === 'error')
                    return true;
                // I do not think this can happen.abbrev. Module call errors are reported in the operation.
                if (reportAction.assert && reportAction.assert.result && reportAction.assert.result === 'error')
                    return true;
                return false;
            },
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
            'testLevelDebugTitle',
        ],
        components: {
            ActionDetails,
            ScriptDetailsContained,
            TestStatusEventWrapper,
            ComponentScript,
            DebuggableListItem,
        },
        mixins: [
            colorizeTestReports,
            debugTestScriptMixin,
        //    importMixin
        ],
        name: "TestDetails"
    }
</script>

<style scoped>

</style>
