<template>
    <div>
        <div v-if="script && report" class="script">
            <!--            <div v-if="script.description">-->
            <!--                {{ script.description }}-->
            <!--            </div>-->
<!--            <div v-for="(fixture, i) in fixtures"-->
<!--                 :key="i">-->
<!--                <span class="name" >Fixture: </span>-->
<!--                <span class="value">{{ fixture.id }}</span>-->
<!--            </div>-->
<!--            <div v-for="(variable, i) in variables"-->
<!--                 :key="'Var' + i">-->
<!--                <span class="name" >Variable: </span>-->
<!--                <span class="value">{{ variable.name }}</span>-->
<!--            </div>-->

            <!--   add SETUP here  -->

            <div v-for="(test, testi) in tests"
                 :key="'Eval' + testi">
                <div>
                    <span class="selectable" @click.self="toggleEventDisplayed()">Message Log</span>
                    <span v-if="eventDisplayed">
                        <img src="../../assets/arrow-down.png" @click.self="toggleEventDisplayed()">
                        <log-item
                                :sessionId="sessionId"
                                :channelId="channelId"
                                :eventId="eventId"
                                :noNav="true">
                        </log-item>
                    </span>
                    <span v-else>
                        <img src="../../assets/arrow-right.png" @click.self="toggleEventDisplayed()">
                    </span>
                </div>

                <!-- actions will be asserts only-->
                <div v-for="(action, actioni) in actions(testi)" class="test-part"
                     :key="'Eval' + testi + 'Action' + actioni">
                    <div>
                        <div >
                            <div @click.self="selectAssert(actioni)" v-bind:class="{
                                    pass: assertResult(testi, actioni) === 'pass',
                                    fail: assertResult(testi, actioni) === 'fail',
                                    error: assertResult(testi, actioni) === 'error',
                                    warning: assertResult(testi, actioni) === 'warning',
                                    'not-run': assertResult(testi, actioni) === 'not-run' }">
                                <span class="selectable">Assert:</span> {{ assertDesc(testi, actioni) }}
                            </div>
                            <div v-if="selectedAssertIndex === actioni" class="message-part">
                                <div v-if="assertRef(testi, actioni)">
                                    {{ assertRef(testi, actioni) }}
                                </div>
                                <ul>
                                    <li v-for="(item, itemi) in assertMessage(testi, actioni)" :key="'AM' + itemi">
                                        {{ item }}
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- add TEARDOWN here -->

        </div>
    </div>
</template>

<script>
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    //import EvalReportAssert from './EvalReportAssert'
    import LogItem from "../logViewer/LogItem"

    export default {
        data() {
            return {
                script: null,
                report: null,
                selectedAssertIndex: null,
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
                if (this.testReport && actionIndex < this.testReport.test[testIndex].action.length) {
                    return this.testReport.test[testIndex].action[actionIndex].assert.message.split("\n")
                }
                return null
            },
            assertResult(testIndex, actionIndex) {
                if (this.testReport && actionIndex < this.testReport.test[testIndex].action.length)
                    return this.testReport.test[testIndex].action[actionIndex].assert.result
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
            selectAssert(assertIndex) {
                if (this.selectedAssertIndex === assertIndex)
                    this.selectedAssertIndex = null
                else
                    this.selectedAssertIndex = assertIndex
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
                return this.$store.dispatch('loadTestScript', { testCollection: this.testCollection, testId: this.testId }).then(() => {
                    this.script = this.$store.state.testRunner.testScripts[this.testId]
                })
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
            reportAction(testi, actioni) {
                if (!this.report)
                    return null
                return this.report.test[testi].action[actioni]
            },
            assertMsg(assertId) {
                return this.$store.state.testRunner.testAssertions[assertId]
            },
            loadReports() {
                return this.$store.dispatch('loadReports', this.testCollection)
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
                }
                // await this.loadReports()
                this.loadTestScript()
                //this.loadTestReport()
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
        mixins: [ errorHandlerMixin ],
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId', 'eventId', 'runEval'
        ],
        components: {
            //EvalReportAssert
            LogItem
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
        cursor: pointer;
        text-decoration: underline;
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
