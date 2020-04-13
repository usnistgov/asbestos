<template>
    <div>
        <div v-for="(caction, cactioni) in scriptActions"
             :key="'CAction' + cactioni">
            <action-details
                    :script="caction"
                    :report="reportActions ? reportActions[cactioni] : null"
                    :debug-title="debugTitle(cactioni)"
                    @onStatusMouseOver="hoverActionIndex = cactioni"
                    @onStatusMouseLeave="hoverActionIndex = -1"
                    @onStatusClick="toggleBreakpointIndex(cactioni)"></action-details>
        </div>
    </div>
</template>

<script>
import ActionDetails from "./ActionDetails";
    const path = require('path')

    export default {
        data() {
            return {
                breakpointIndex: [],
            }
        },
        name: "ComponentScript",
        methods: {
            debugTitle(idx) {
                if (! this.breakpointIndex[idx]) {
                    return "Set breakpoint"
                } else {
                    return "Remove breakpoint"
                }
            },
            isBreakpoint(actionIdx) {
                return Boolean(this.breakpointIndex[actionIdx]) || ! this.breakpointIndex[actionIdx] && this.hoverActionIndex === actionIdx
            },
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
        },
        computed: {
            componentName() {
                if (this.actionReport === null) return null;
                const moduleId = this.moduleId;
                if (!moduleId) return null;
                return this.testId + path.sep + moduleId;
            },
            moduleId() {
                if (!this.actionReport) return null;
                if (!this.actionReport.operation) return null;
                if (!this.actionReport.operation.extension) return null;
                let moduleId = null;
                this.actionReport.operation.extension.forEach(extension => {
                    if (extension.url === 'urn:moduleId')
                        moduleId = extension.valueString;
                })
                return moduleId;
            },
            scriptActions() {   // component has no setup and a single test
                if (this.componentName === null) return null
                const script = this.$store.state.testRunner.moduleTestScripts[this.componentName]
                if (!script || !script.test || !script.test[0]) return null
                return script.test[0].action
            },
            reportActions() {  // component has no setup and a single test
                if (this.componentName === null) return null
                const report = this.$store.state.testRunner.moduleTestReports[this.componentName]
                if (!report || !report.test || !report.test[0]) return null
                return report.test[0].action
            },
            testId() {
                return this.$store.state.testRunner.currentTest
            },
        },
        props: [
            'actionScript', 'actionReport'
        ],
        components: {
            ActionDetails
        }
    }
</script>

<style scoped>

</style>
