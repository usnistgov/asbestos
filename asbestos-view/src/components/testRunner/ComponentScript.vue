<template>
    <ul class="importedTestScriptList">
        <debuggable-list-item
                v-for="(caction, cactioni) in scriptActions"
                :key="'CAction' + cactioni"
                :breakpoint-index="parentIndex + '/' + getBreakpointIndex('test', 0, cactioni)">

            <action-details
                    :script="caction"
                    :report="reportActions ? reportActions[cactioni] : null"
                    :calling-script="actionScript"
                    :module-script="moduleScript"
                    >
            </action-details>
        </debuggable-list-item>
    </ul>
</template>

<script>
import ActionDetails from "./ActionDetails";
import DebuggableListItem from "./debugger/DebuggableListItem";
import debugTestScriptMixin from "../../mixins/debugTestScript";

    export default {
        data() {
            return {
            }
        },
        name: "ComponentScript",
        methods: {
        },
        computed: {
          moduleScript() {
            if (this.componentName)
              return this.$store.state.testRunner.moduleTestScripts[this.componentName];
            return null;
          },
            // if a component is used multiple times then the componentName is the same and the componentId is different
            componentName() {
                if (this.actionReport === null) return null;
                const moduleName = this.moduleName;
                if (!moduleName) return null;
                return this.testId + '/' + moduleName;
            },
            componentId() {
                if (this.actionReport === null) return null;
                const moduleId = this.moduleId;
                if (!moduleId) return null;
                return this.testId + '/' + moduleId;
            },
            moduleId() {
                if (!this.actionReport) return null;
                if (!this.actionReport.operation) return null;
                if (!this.actionReport.operation.modifierExtension) return null;
                let moduleId = null;
                this.actionReport.operation.modifierExtension.forEach(extension => {
                    if (extension.url === 'urn:moduleId')
                        moduleId = extension.valueString;
                })
                return moduleId;
            },
            moduleName() {
                if (!this.actionReport) return null;
                if (!this.actionReport.operation) return null;
                if (!this.actionReport.operation.modifierExtension) return null;
                let moduleName = null;
                this.actionReport.operation.modifierExtension.forEach(extension => {
                    if (extension.url === 'urn:moduleName')
                        moduleName = extension.valueString;
                })
                return moduleName;
            },
            scriptActions() {   // component has no setup and a single test
                // if (this.componentName === null) return null
                const myModuleId = this.testId + '/' + this.actionComponentName
                const script = this.$store.state.testRunner.moduleTestScripts[myModuleId]
                if (!script || !script.test || !script.test[0]) return null
                return script.test[0].action
            },
            reportActions() {  // component has no setup and a single test
                // for server tests the module-report is in separate file (hense moduleTestReports)
                // but for client tests it is compressed all into one report file
                if (this.componentId === null) return null
                const report = this.$store.state.testRunner.moduleTestReports[this.componentId]
                if (!report || !report.test || !report.test[0]) return null
                return report.test[0].action
            },
            testId() {
                return this.$store.state.testRunner.currentTest
            },
        },
        props: [
            'actionScript',
             'actionReport',
            'actionComponentName',
            'parentIndex',
        ],
        components: {
            ActionDetails,
            DebuggableListItem,
        },
        mixins: [
            debugTestScriptMixin,
        ],
    }
</script>

<style scoped>
</style>
<style>
    .importedTestScriptList {
        list-style:none;
        padding-left: 0px;
    }
</style>
