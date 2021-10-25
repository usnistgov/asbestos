<template>
    <ul v-bind:class="[evalTestId  ? 'evalImportedTestScriptList' : 'importedTestScriptList', '']">
        <debuggable-list-item
                v-for="(caction, cactioni) in scriptActions"
                :key="'CAction' + cactioni"
                :breakpoint-index="parentIndex + '/' + getBreakpointIndex('test', 0, cactioni)"
                :is-disabled="disableDebugger">

            <div v-for="(resultObj, resultKey) in scriptImport(caction)" :key="resultKey">
                <div v-if="reportAction(cactioni) && reportContainsError(reportAction(cactioni))">
                    <action-details
                            :script="caction"
                            :report="reportAction(cactioni)"
                            :calling-script="actionScript"
                            :module-script="moduleScript"
                    >
                    </action-details>

                </div>
                <div v-else-if="'hasImport' in resultObj && resultObj.hasImport">
                   <test-or-eval-details
                       :session-id="$store.state.base.channel.testSession"
                       :channel-name="$store.state.base.channel.channelName"
                       :test-collection="$store.state.testRunner.currentTestCollectionName"
                       :test-id="testId.concat('/').concat(resultObj.componentName)"
                       :disable-debugger="'true'"
                       ></test-or-eval-details>
                </div>
                <div v-else class="has-cursor">
                    <action-details
                            :script="caction"
                            :report="reportAction(cactioni)"
                            :calling-script="actionScript"
                            :module-script="moduleScript"
                    >
                    </action-details>

                </div>
            </div>

        </debuggable-list-item>
    </ul>
</template>

<script>
import ActionDetails from "./ActionDetails";
import DebuggableListItem from "./debugger/DebuggableListItem";
import debugTestScriptMixin from "../../mixins/debugTestScript";
import importMixin from "../../mixins/importMixin";

    export default {
        data() {
            return {
            }
        },
        name: "ComponentScript",
        methods: {
            reportAction(componentActionidx) {
                return this.reportActions ? this.reportActions[componentActionidx] : null
            }
        },
        computed: {
          moduleScript() {
            if (this.componentName) {
                // console.log('in ComponentScript. componentName: ' + this.componentName)
                // console.log(this.$store.state.testRunner.moduleTestScripts[this.componentName])
                return this.$store.state.testRunner.moduleTestScripts[this.componentName];
            }
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
                if (this.testId == null || this.testId == undefined) {
                    console.error('Incorrect testId, value is: ' + this.testId)
                    return null;
                }
                const myModuleId = this.testId + '/' + this.actionComponentName
                const script = this.$store.state.testRunner.moduleTestScripts[myModuleId]
                if (!script || !script.test || !script.test[0]) return null
                return script.test[0].action
            },
            reportActions() {  // component has no setup and a single test
                // for server tests the module-report is in separate file (hence moduleTestReports)
                // but for client tests it is compressed all into one report file
                // console.log('reportActions componentId is ' + this.componentId)
                if (this.componentId === null) return null
                const report = this.$store.state.testRunner.moduleTestReports[this.componentId]
                if (!report || !report.test || !report.test[0]) {
                    console.error('report is not usable')
                    return null
                }
                return report.test[0].action
            },
            testId() {
                if (this.evalTestId !== '' && this.evalTestId !== null && this.evalTestId !== undefined) {
                    // For client test
                    return this.evalTestId
                } else {
                  // For client eval tests, currentTest is not set
                  // Use this for normal test run
                  return this.$store.state.testRunner.currentTest
                }
            },
        },
        props: [
            'actionScript',
             'actionReport',
            'actionComponentName',
            'parentIndex',
            'disableDebugger',
            'evalTestId',
        ],
        components: {
            TestOrEvalDetails: () => import('./TestOrEvalDetails'),
            ActionDetails,
            DebuggableListItem,
        },
        mixins: [
            debugTestScriptMixin,
            importMixin,
        ],
    }
</script>

<style scoped>
</style>
<style>
    .evalImportedTestScriptList,
    .importedTestScriptList {
        list-style:none;
        padding-left: 0px;
    }
    .evalImportedTestScriptList {
        margin: 0px 0px 16px 0px;
    }
</style>
