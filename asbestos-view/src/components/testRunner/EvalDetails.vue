<template>
    <div>
        <div v-if="script && report" class="script">
            <!--   add SETUP here  -->
            <div>
                <span v-if="inspectorOpen">
                    <img src="../../assets/arrow-down.png" @click.self="toggleInspectorOpen()">
                </span>
                <span v-else>
                    <span v-if="!noInspectLabel">
                        <img src="../../assets/arrow-right.png" @click.self="toggleInspectorOpen()">
                    </span>
                </span>

                <span v-if="!noInspectLabel" class="selectable" @click.self="toggleInspectorOpen()">Inspect</span>
                <span v-if="inspectorOpen">
                        <inspect-event
                                :sessionId="sessionId"
                                :channelName="channelName"
                                :eventId="eventId"
                                :noNav="true">
                        </inspect-event>
                    </span>
            </div>


            <div v-for="(test, testi) in tests" class="test-part"
                 :key="'Eval' + testi">    <!--   v-bind:class="testResult(testi) + ((colorful)?'':'plain-detail')"   -->
                <div >{{test.description}}</div>

                <!-- actions will be asserts only-->
                <div v-for="(action, actioni) in actions(testi)" class="assert-part"
                     :key="'Eval' + testi + 'Action' + actioni">

                    <eval-action-details
                            :script="action"
                            :report="reportAction(testi, actioni)"
                    > </eval-action-details>
                </div>
            </div>

            <!-- add TEARDOWN here -->

        </div>
        <div v-else>
            Not Available
        </div>
    </div>
</template>

<script>
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    import colorizeTestReports from "../../mixins/colorizeTestReports";
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
                inspectorOpen: false,
            }
        },
        methods: {
            reportAction(testi, actioni) {
                return this.report.test[testi].action[actioni]
            },
            toggleInspectorOpen() {
                this.inspectorOpen = !this.inspectorOpen
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
            operationOrAssertion(testi, actioni) {
                const action = this.script.test[testi].action[actioni]
                return action.operation ? `Operation: ${this.operationType(action.operation)}` : `Assert: ${this.assertionDescription(action.assert)}`
            },
            operationType(operation) {
                return operation && operation.type ? operation.type.code : null
            },
            assertionDescription(assert) {
                return assert.description === undefined ? "" : assert.description
            },
            async loadTestScript() {
                if (this.modalMode === undefined || this.modalMode==='') {
                    this.$store.commit('setTestCollectionName', this.testCollection)
                    await this.$store.dispatch('loadTestScripts', [this.testId]);
                    this.script = this.$store.state.testRunner.testScripts[this.testId]
                }
            },
            loadTestReport() {
              console.log(`loadTestReport`)
                const reportsForTest = this.$store.state.testRunner.clientTestResult[this.testId]
                if (!reportsForTest)
                  return;
                console.log(`got reportsForTest`)
                const reportsForEvent = reportsForTest[this.eventId];
                if (!reportsForEvent)
                  return;
                console.log(`got reportsForEvent - length is ${reportsForEvent.length}`)
                if (reportsForEvent.length > 0)
                  this.report = reportsForEvent[0];
            },
            actions(testIndex) {
                return this.script.test[testIndex].action === undefined ? [] : this.script.test[testIndex].action
            },
            async runSingleEventEval() {
                await this.$store.dispatch('runSingleEventEval',
                    {
                        testId: this.testId,
                        eventId: this.eventId,
                        testCollectionName: this.testCollection
                    })
              console.log(`runSingleEventEval done`)
            },
            async testOrEventUpdated() {
                if (this.runEval) {
                    if (!this.$store.state.testRunner.testAssertions)
                        await this.$store.dispatch('loadTestAssertions')
                    await this.runSingleEventEval();
                    await this.loadTestScript();
                    this.loadTestReport();
                } else {
                    await this.$store.dispatch('runEval', this.testId)
                    await this.loadTestScript()
                    this.loadTestReport()
                }
            }
        },
        computed: {
            assertProfile() {
                return this.$store.state.testRunner.testAssertions['Profile']
            },
            fixtures() {
                return this.script.fixture
            },
            variables() {
                return this.script.variable
            },
            tests() {
                return this.script.test
            },
            testScript() {
                return this.$store.state.testRunner.testScripts[this.testId]
            },
            testReport() {
                return this.$store.state.testRunner.clientTestResult[this.testId][this.eventId]
            },
            testReports() {  // see watch of the same name
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
            'sessionId', 'channelName', 'testCollection', 'testId', 'eventId', 'runEval', 'noInspectLabel', 'modalMode'
        ],
        components: {
            //EvalReportAssert

            InspectEvent: () => import('../logViewer/InspectEvent'),
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
