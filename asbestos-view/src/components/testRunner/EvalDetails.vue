<template>
    <div>
        <div v-if="script && report" class="script">
            <!--   add SETUP here  -->
            <div>
                <span v-if="eventDisplayed">
                    <img src="../../assets/arrow-down.png" @click.self="toggleEventDisplayed()">
                </span>
                <span v-else>
                    <span v-if="!noInspectLabel">
                        <img src="../../assets/arrow-right.png" @click.self="toggleEventDisplayed()">
                    </span>
                </span>
                <span v-if="!noInspectLabel" class="selectable" @click.self="toggleEventDisplayed()">Inspect</span>
                <span v-if="eventDisplayed">
                        <log-item
                                :sessionId="sessionId"
                                :channelId="channelId"
                                :eventId="eventId"
                                :noNav="true">
                        </log-item>
                    </span>
            </div>
            <div v-for="(test, testi) in tests" class="test-part"
                 :key="'Eval' + testi">
                <div v-bind:class="testResult(testi) + ((colorful)?'':'plain-detail')">{{test.description}}</div>

                <!-- actions will be asserts only-->
                <div v-for="(action, actioni) in actions(testi)" class="assert-part"
                     :key="'Eval' + testi + 'Action' + actioni">

                    <eval-action-details
                            :script="testScript"
                            :report="testReport"
                    > </eval-action-details>



<!--                    <div>-->
<!--                        <div >-->
<!--                            <div @click.self="selectAssert(testi, actioni)" v-bind:class="{-->
<!--                                    pass: assertResult(testi, actioni) === 'pass' && colorful,-->
<!--                                    'pass-plain': assertResult(testi, actioni) === 'pass' && !colorful,-->
<!--                                    fail: assertResult(testi, actioni) === 'fail' && colorful,-->
<!--                                    'fail-plain': assertResult(testi, actioni) === 'fail' && !colorful,-->
<!--                                    error: assertResult(testi, actioni) === 'error' && colorful,-->
<!--                                    'error-plain': assertResult(testi, actioni) === 'error' && !colorful,-->
<!--                                    warning: assertResult(testi, actioni) === 'warning' && colorful,-->
<!--                                    'not-run': assertResult(testi, actioni) === 'not-run'  && colorful-->
<!--                            }">-->
<!--                                <span class="selectable">Assert:</span> {{ assertDesc(testi, actioni) }}-->
<!--                            </div>-->

<!--                            <test-status v-if="!statusRight"-->
<!--                                         :status-on-right="statusRight"-->
<!--                                         :report="report"-->
<!--                            > </test-status>-->

<!--                            <div v-if="selectedTestIndex === testi && selectedAssertIndex === actioni" class="message-part">-->
<!--                                <div v-if="assertRef(testi, actioni)">-->
<!--                                    {{ assertRef(testi, actioni) }}-->
<!--                                </div>-->
<!--                                <ul>-->
<!--                                    <li v-for="(item, itemi) in assertMessage(testi, actioni)" :key="'AM' + itemi">-->
<!--                                        {{ item }}-->
<!--                                    </li>-->
<!--                                </ul>-->
<!--                            </div>-->

<!--                            <test-status v-if="statusRight"-->
<!--                                         :status-on-right="statusRight"-->
<!--                                         :report="report"-->
<!--                            > </test-status>-->

<!--                        </div>-->
<!--                    </div>-->
                </div>
            </div>

            <!-- add TEARDOWN here -->

        </div>
        <div v-else>
            Debug
        </div>
    </div>
</template>

<script>
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import LogItem from "../logViewer/LogItem"
    //import TestStatus from "./TestStatus";
    import EvalActionDetails from "./EvalActionDetails";

    export default {
        data() {
            return {
                script: null,
                report: null,
                selectedAssertIndex: null,
                selectedTestIndex: null,
                passClass: 'pass',
                failClass: 'fail',
                eventDisplayed: false,
            }
        },
        methods: {
            toggleEventDisplayed() {
                this.eventDisplayed = !this.eventDisplayed
            },
            assertMessage(testIndex, actionIndex) {
                if (this.testReport  &&  actionIndex < this.testReport.test[testIndex].action.length) {
                    return this.testReport.test[testIndex].action[actionIndex].assert.message.split("\n")
                }
                return null
            },
            testResult(testIndex) {
                let assertIndex
                for (assertIndex=0; assertIndex<this.actions(testIndex).length; assertIndex++) {
                    const result = this.assertResult(testIndex, assertIndex)
                    if (result === 'pass')
                        continue
                    if (result === 'warning')
                        continue
                    return 'fail'
                }
                return 'pass'
            },
            assertResult(testIndex, actionIndex) {
                if (this.report && actionIndex < this.report.test[testIndex].action.length)
                    return this.report.test[testIndex].action[actionIndex].assert.result
                return 'not-run'
            },
            assertReport(testIndex, actionIndex) {
                return this.testReport ? this.testReport.test[testIndex].action[actionIndex].assert : null
            },
            assertScript(testIndex, actionIndex) {
                return this.testScript.test[testIndex].action[actionIndex].assert
            },
            assertDesc(testIndex, actionIndex) {
                const rawDesc = this.assertScript(testIndex, actionIndex).description
                if (!rawDesc.includes("|"))
                    return rawDesc
                const elements = rawDesc.split("|")
                const msg = elements[0]
                return msg
            },
            assertRef(testIndex, actionIndex) {
                const rawDesc = this.assertScript(testIndex, actionIndex).description
                if (!rawDesc.includes("|"))
                    return ''
                const elements = rawDesc.split("|")
                const assertId = elements[1]
                return `Reference: ${this.assertProfile} - ${this.assertMsg(assertId)}\n`
            },
            selectAssert(testIndex, assertIndex) {
                if (this.selectedTestIndex === testIndex && this.selectedAssertIndex === assertIndex) {
                    this.selectedTestIndex = null
                    this.selectedAssertIndex = null
                } else {
                    this.selectedTestIndex = testIndex
                    this.selectedAssertIndex = assertIndex
                }
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
                //await this.$store.dispatch('loadTestScripts', this.$store.state.testRunner.testScriptNames)
                this.script = this.$store.state.testRunner.testScripts[this.testId]
            },
            loadTestReport() {
                const reports = this.$store.state.testRunner.clientTestResult[this.testId]
                if (reports)
                    this.report = reports[this.eventId]
            },
            actions(testIndex) {
                return this.script.test[testIndex].action === undefined ? [] : this.script.test[testIndex].action
            },
            scriptAction(testi, actioni) {
                return this.script.test[testi].action[actioni]
            },
            reportAction(testi, actioni) {
                if (!this.report)
                    return null
                return this.report.test[testi].action[actioni]
            },
            assertMsg(assertId) {
                return this.$store.state.testRunner.testAssertions[assertId]
            },
            runSingleEventEval() {
                this.$store.dispatch('runSingleEventEval',
                    {
                        testId: this.testId,
                        eventId: this.eventId,
                        testCollectionName: this.testCollection
                    })
            },
            async testOrEventUpdated() {
                if (this.runEval) {
                    if (!this.$store.state.testRunner.testAssertions)
                        this.$store.dispatch('loadTestAssertions')
                    await this.runSingleEventEval()
                    await this.loadTestScript()
                } else {
                    await this.$store.dispatch('runEval', this.testId)
                    this.loadTestScript()
                    this.loadTestReport()
                }
            }
        },
        computed: {
            assertProfile() {
                return this.$store.state.testRunner.testAssertions['Profile']
            },
            // selected() {
            //     return this.$store.state.testRunner.currentAssertIndex
            // },
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
            testScript() {
                return this.$store.state.testRunner.testScripts[this.testId]
            },
            testReport() {
                return this.$store.state.testRunner.clientTestResult[this.testId][this.eventId]
            },
            testReports() {  // see watch
                return this.$store.state.testRunner.testReports[this.testId]
            }
        },
        created() {
           this.testOrEventUpdated()
        },
        mounted() {

        },
        watch: {
            'testId': 'testOrEventUpdated',
            'eventId': 'testOrEventUpdated',
            testReports() {  // this has same name as computed - see https://stackoverflow.com/questions/43270159/vuejs-2-how-to-watch-store-values-from-vuex
                this.loadTestReport()
            }
        },
        mixins: [ errorHandlerMixin, colorizeTestReports ],
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId', 'eventId', 'runEval', 'noInspectLabel',
        ],
        components: {
            //EvalReportAssert
            LogItem,
           // TestStatus,
            EvalActionDetails
        },
        name: "EvalDetails"
    }
</script>

<style scoped>
    .script {
        margin-left: 15px;
        margin-right: 15px;
        text-align: left;
    }
    .test-part {
        margin-left: 20px;
        margin-right: 20px;
    }
    .assert-part {
        margin-left: 20px;
        margin-right: 20px;
        /*cursor: pointer;*/
        /*text-decoration: underline;*/
    }
    .message-part {
        margin-left: 25px;
        margin-right: 25px;
        background-color: white;
    }
    .name {
        font-weight: bold;
    }
    .value {

    }
    .pass {
        background-color: lightgreen;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .fail {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .warning {
        background-color: #F6C6CE;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .error {
        background-color: cornflowerblue ;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .not-run {
        background-color: lightgray;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
</style>
