<template>
    <div>
        <div v-if="script" class="script">
            <div>
                <span class="selectable" @click="toggleScriptViewer()">Test Viewer</span>
                <span v-if="displayScriptViewer">
                    <img src="../../assets/arrow-down.png" @click="toggleScriptViewer()">
                    <test-script
                            :script="script"
                            :report="report">
                    </test-script>
                </span>
                <span v-else>

                    <img src="../../assets/arrow-right.png" @click="toggleScriptViewer()">
                </span>
            </div>


            <div v-if="displayDetail">
                <div v-for="(fixture, i) in fixtures"
                     :key="i">
                    <span class="name" >Fixture: </span>
                    <span class="value">{{ fixture.id }}</span>
                </div>
                <div v-for="(variable, i) in variables"
                     :key="'Var' + i">
                    <span class="name" >Variable: </span>
                    <span class="value">{{ variable.name }}</span>
                </div>
            </div>
            <!--   Setup is used for two things.
                   1. Actual setup so there is script and report
                   2. Deep errors and the error is reported in report.setup (with no corresponding script)
                   -->
            <div v-if="script.setup && report && report.setup">
                <!-- don't need yet -->
            </div>

            <div v-if="!script.setup && report && report.setup">
                <test-report-action
                        :script="null"
                        :report="report.setup.action">
                </test-report-action>
            </div>


            <div v-for="(test, testi) in tests"
                 :key="'Test' + testi">
                    <template v-if="test.description" class="test-part">
                        {{ test.description }}
                    </template>
                <div>
<!--                    Contained: {{ findContained(script, containedTests(testi)) }}-->
                        <div v-for="(test2, test2i) in containedTests(findContained(script, containedTestsRef(script, testi)))"
                             :key="'Test2' + test2i">
                            <template v-if="test2.description" class="test-part">
                                {{ test2.description }}
                            </template>
                            <div v-for="(action, actioni) in test2.action" class="test-part"
                                 :key="'Test' + testi + 'Action' + actioni">
                                <test-report-action
                                        :script="action"
                                        :report="findContained(report, containedTestsRef(report, testi)).test[test2i].action[actioni]"> </test-report-action>
                            </div>
                        </div>
                    main
                    <div v-for="(action, actioni) in actions(testi)" class="test-part"
                         :key="'Test' + testi + 'Action' + actioni">
                        <test-report-action
                                :script="action"
                                :report="reportAction(report, testi, actioni)"> </test-report-action>
                    </div>
                </div>
            </div>

            <!-- add TEARDOWN here -->

        </div>
    </div>
</template>

<script>
    import {ENGINE} from '../../common/http-common'
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    import TestReportAction from './TestReportAction'
    import TestScript from "../testViewer/TestScript"
    export default {
        data() {
            return {
                script: null,
                report: null,
                displayDetail: false,
                displayScriptViewer: false,
            }
        },
        methods: {
            toggleScriptViewer() {
                this.displayScriptViewer = !this.displayScriptViewer
            },
            containedTestsRef(script_or_report, testi) {  // script or report
                if (!script_or_report.test)
                    return null
                const mainTest = script_or_report.test[testi]
                return this.containedTestReference(mainTest)
            },
            containedTestReference(test) {
                if (!test.modifierExtension)
                    return null
                const ref = test.modifierExtension[0].valueReference.reference
                return ref.substring(1)
            },
            findContained(obj, id) {
                if (id && id === 'foo')
                    return 'foo'  // just to preserve parm id
                if (!obj.contained) {
                    return null
                }
                return obj.contained[0]
            },
            containedTests(script) {  // or report
                if (script)
                    return script.test
                return null
            },
            operationOrAssertion(testi, actioni) {
                const action = this.script.test[testi].action[actioni]
                return action.operation ? `Operation: ${this.operationType(action.operation)}` : `Assert: ${this.assertionDescription(action.assert)}`
            },
            operationType(operation) {
                return operation.type.code
            },
            assertionDescription(assert) {
                return assert.description === undefined ? "" : assert.description
            },
            loadTestScript() {
                if (this.$store.state.testRunner.testScripts[this.testId] === undefined) {
                    const that = this
                    ENGINE.get(`collection/${this.testCollection}/${this.testId}`)
                        .then(response => {
                            this.$store.commit('addTestScript', {name: this.testId, script: response.data})
                            this.script = response.data
                        })
                        .catch(function (error) {
                            that.error(error)
                        })
                } else {
                    this.script = this.$store.state.testRunner.testScripts[this.testId]
                }
            },
            loadTestReport() {
                this.report = this.$store.state.testRunner.testReports[this.testId]
            },
            actions(testIndex) {
                return this.script.test[testIndex].action
            },
            scriptAction(testi, actioni) {
                return this.script.test[testi].action[actioni]
            },
            reportAction(report, testi, actioni) {
                if (!report)
                    return null
                if (!report.test)
                    return null
                if (!report.test[testi])
                    return null
                return report.test[testi].action[actioni]
            },

        },
        computed: {
            fixtures() {
                return this.script.fixture
            },
            variables() {
                return this.script.variable
            },
            tests() {
                return this.script.test
            },
            current() {
                return this.$store.state.base.testCollectionDetails.find(item => {
                    return item.name === this.testId
                })
            },
        },
        created() {
            this.loadTestScript()
            this.loadTestReport()
        },
        mounted() {

        },
        watch: {
            'testId': 'loadTestReport'
        },
        mixins: [ errorHandlerMixin ],
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId'
        ],
        components: {
            TestReportAction,
            TestScript,
        },
        name: "TestDetails"
    }
</script>

<style scoped>
.script {
    text-align: left;
}
.test-part {
    margin-left: 20px;
    margin-right: 20px;
}
    .name {
        font-weight: bold;
    }
    .value {

    }

</style>
