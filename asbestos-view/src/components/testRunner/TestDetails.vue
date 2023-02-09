<template>
    <div class="inlineDiv">
        <span v-bind:class="{
            'pass':         isPass  && colorful,
            'pass-plain-detail': isPass  && !colorful,
            'fail':         isFail  && colorful,
            'fail-plain-detail': isFail  && !colorful,
             'error':       isError && colorful,
             'error-plain': isError && !colorful,
             'not-run':   isNotRun && colorful,
             'not-run-plain-detail' : isNotRun && ! colorful,
             'breakpointHitBkg' : $parent && $parent.isBreakpointHit,
            }"  @click.stop="toggleDisplay">

            <test-status v-if="!statusRight"
                         :status-on-right="statusRight"
                         :script="script"
                         :report="report"
            > </test-status>

            <span v-if="displayOpen">
                <img src="../../assets/arrow-down.png">
            </span>
            <span v-else>
                <img src="../../assets/arrow-right.png"/>
            </span>

            <span class="has-cursor">
                <span v-if="label">{{label}}: </span>
                <span v-else-if="isConditional" class="bold">If: </span>
              <span v-if="name">{{name}}</span>
              <span v-else>{{description}}</span>
            </span>
        </span>

      <div v-if="displayOpen && documentation" class="indent">
        <vue-markdown>{{ documentation }}</vue-markdown>
      </div>
      <div v-if="displayOpen && name && description" class="indent">
        {{ description }}
      </div>

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
                    :breakpoint-index="getParentBreakpointIndex(parentTestIndex, testType, testIndex, actioni)"
                    :is-import-header="scriptImport(action).result.hasImport"
                    :is-disabled="disableDebugger"
            >
                <!--                    :breakpoint-index="getBreakpointIndex(testType, testIndex, actioni)"-->
                    <div v-for="(resultObj, resultKey) in scriptImport(action)" :key="resultKey">
                        <div v-if="report && report.action && report.action[actioni] && reportContainsError(report.action[actioni])">
                            <action-details
                                :script="action"
                                :report="report && report.action ? report.action[actioni] : null"
                                >
                            </action-details>
                        </div>
                        <div v-else-if="'hasImport' in resultObj && resultObj.hasImport">
                            <!--
                            debug script

                            hasImportZZ{{testType}}
                            {{action}}
                            {{report}}
                            -->
                            <!--
                            debug breakpoint

                            {{parentTestIndex}}
                            {{testType}}
                            {{testIndex}}
                            {{actioni}}
                            -->
                            <component-script
                               :action-script="action"
                               :action-report="report && report.action ? report.action[actioni] : null"
                               :action-component-name="resultObj.componentName"
                               :parent-index="getParentBreakpointIndex(parentTestIndex, testType, testIndex, actioni)"
                               :disable-debugger="disableDebugger"
                               :test-type="testType"
                               :eval-test-id="evalTestId"
                            ></component-script>
                        </div>
                        <div v-else class="has-cursor">
                            <!--
                            debug breakpoint

                            {{parentTestIndex}}--
                            {{testType}}
                            {{testIndex}}
                            {{actioni}}
                            -->

                            <action-details
                               :script="action"
                               :report="report && report.action ? report.action[actioni] : null"
                            >
                           </action-details>
                        </div>
                    </div>
            </debuggable-list-item>
        </ul>
    </div>
</template>

<script>
    import ActionDetails from './ActionDetails'
    import ScriptDetailsContained from "./ScriptDetailsContained";
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import TestStatus from "./TestStatus";
    import VueMarkdown from "vue-markdown";

    import ComponentScript from "./ComponentScript";
    import DebuggableListItem from "./debugger/DebuggableListItem";
    import debugTestScriptMixin from "../../mixins/debugTestScript";
    import importMixin from "../../mixins/importMixin";

    export default {
        data() {
            return {
                displayOpen: false,
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
            toggleDisplay() {
                this.displayOpen = !this.displayOpen
                this.displayAdditionalIndexLabel(this.displayOpen, this.getBreakpointIndex(this.testType, this.testIndex))
            },
          // async loadFullScript() { // not used - script loaded earlier
          //   //this.$store.commit('setTestCollectionName', this.testCollection)
          //   await this.$store.dispatch('loadTestScripts', [this.$store.state.testRunner.currentTest]);
          //   this.script = this.$store.state.testRunner.testScripts[this.testId]
          // },
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
          name() {
            if (!this.script) return ""
            return this.script.name
          },
          documentation() {
            if (!this.script.extension) return "";
            if (this.script.extension[0].url === 'urn:documentation') {
              return this.script.extension[0].valueString;
            }
            return "";
          },
        },
        created() {
        },
        props: [
            // parts representing a single test element of a TestScript
            'script', 'report',
            'scriptContained', 'reportContained', // contained section of the TestScript and TestReport - used in conditional
            'label',
            'testIndex', 'testType', 'disableDebugger', 'parentTestIndex', 'evalTestId'
        ],
        components: {
            ActionDetails,
            ScriptDetailsContained,
            ComponentScript,
            TestStatus,
            DebuggableListItem,
          VueMarkdown,
        },
        mixins: [
            colorizeTestReports,
            debugTestScriptMixin,
            importMixin
        ],
        name: "TestDetails"
    }
</script>

<style scoped>

</style>
