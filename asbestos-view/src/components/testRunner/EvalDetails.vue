<template>
    <span>
        <span v-if="hasErrors()"><img src="../../assets/cross.png"></span>
        <span v-else><img src="../../assets/check.png"></span>

        <div class="divider"></div>
        <div class="divider"></div>
        <span class="has-cursor details">
            <span @click.stop="open = !open">
                <span v-if="caption" class="caption">
                    {{caption}}
                </span>
                <span v-else class="caption">
                    Details
                </span>
            </span>
            <span v-if="open"><img src="../../assets/arrow-down.png" @click.stop="open = !open"></span>
            <span v-else><img src="../../assets/arrow-right.png"  @click.stop="open = !open"></span>
        </span>
        <div v-if="script && report && open" class="script">
            <!--   add SETUP here  -->
            <div>
                <span v-if="!noInspectLabel" class="selectable" @click.self="toggleEventDisplayed()">Inspect</span>
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
                    <span v-if="!noInspectLabel">
                        <img src="../../assets/arrow-right.png" @click.self="toggleEventDisplayed()">
                    </span>
                </span>
            </div>
            <div v-for="(test, testi) in tests" class="test-part"
                 :key="'Eval' + testi">
                <div v-bind:class="testResult(testi)">{{test.name}}</div>

                <!-- actions will be asserts only-->
                <div v-for="(action, actioni) in actions(testi)" class="assert-part"
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
    </span>
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
                open: false,
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
            async loadTestScript() {
                await this.$store.dispatch('loadTestScript', { testCollection: this.testCollection, testId: this.testId })
                this.script = this.$store.state.testRunner.testScripts[this.testId]
            },
            loadTestReport() {
                this.report = this.$store.state.testRunner.testReports[this.testId]
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
            async loadReports() {
                await this.$store.dispatch('loadReports', this.testCollection)
                await this.loadTestScript()
            },
            // runSingleEventEval() {
            //     this.$store.dispatch('runSingleEventEval',
            //         {
            //             testId: this.testId,
            //             eventId: this.eventId,
            //             testCollectionName: this.testCollection
            //         })
            // },
            async testOrEventUpdated() {
                if (this.runEval) {
                    if (!this.$store.state.testRunner.testAssertions)
                        await this.$store.dispatch('loadTestAssertions')
                    await this.$store.dispatch('runSingleEventEval',
                        {
                            testId: this.testId,
                            eventId: this.eventId,
                            testCollectionName: this.testCollection
                        })
                    await this.loadTestScript()
                } else {
                    await this.loadReports()
                    this.loadTestScript()
                    //this.loadTestReport()
                }
            },
            hasErrors() {
                if (!this.$store.state.testRunner.testReports[this.testId]) {
                    this.$store.dispatch('runSingleEventEval',
                        {
                            testId: this.testId,
                            eventId: this.eventId,
                            testCollectionName: this.testCollection
                        })
                }
                return this.$store.getters.clientTestHasErrors(this.testId)
            },
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
            this.open = this.startOpen
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
            'sessionId', 'channelId', 'testCollection', 'testId', 'eventId', 'runEval', 'noInspectLabel', 'startOpen', 'caption',
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
    .details {
        font-size: smaller;
    }
    .caption {
        text-decoration: underline;
        cursor: pointer;
    }
</style>
