<template>
    <div>
        <div v-if="script" class="script">
<!--            <div v-if="script.description">-->
<!--                {{ script.description }}-->
<!--            </div>-->
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

            <!--   add SETUP here  -->

            <div v-for="(test, testi) in tests"
                 :key="'Eval' + testi">
                <span class="name" >Eval: </span>
                <span class="value">Test: {{ test.name }}</span>
                <div v-if="test.description" class="test-part">
                    Test: {{ test.description }}
                </div>

<!--                actions will be asserts only-->
                <div v-for="(action, actioni) in actions(testi)" class="test-part"
                     :key="'Eval' + testi + 'Action' + actioni">
                    <div>
                        <div @click="selectAssert(actioni)">
<!--                            <div v-bind:class="[assertPass(actioni) ? passClass : failClass, 'assert-part']">-->
                            <div v-bind:class="{
                            pass: assertResult(actioni) === 'pass',
                            fail: assertResult(actioni) === 'fail',
                            error: assertResult(actioni) === 'error',
                            'not-run': assertResult(actioni) === 'not-run' }">
                                Assert: {{ assertScript(actioni).description }}
                            </div>
                            <div v-if="selectedAssertIndex === actioni" class="message-part">
                                {{ assertMessage(actioni) }}
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
    import errorHandlerMixin from '../mixins/errorHandlerMixin'
    //import EvalReportAssert from './EvalReportAssert'

    export default {
        data() {
            return {
                script: null,
                report: null,
                selectedAssertIndex: null,
                passClass: 'pass',
                failClass: 'fail',
            }
        },
        methods: {
            assertMessage(assertIndex) {
                return this.assertReport(assertIndex).message
            },
            assertResult(assertIndex) {
                return this.assertReport(assertIndex).result
            },
            assertPass(assertIndex) {
                return this.assertReport(assertIndex).result === 'pass'
            },
            assertReport(assertIndex) {
                return this.testReport.test[0].action[assertIndex].assert
            },
            assertScript(assertIndex) {
                return this.testScript.test[0].action[assertIndex].assert
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
                console.info(`load testscript ${this.testId} - ${this.$store.state.testRunner.testScripts[this.testId]}`)
                return this.$store.dispatch('loadTestScript', { testCollection: this.testCollection, testId: this.testId }).then(() => {
                    this.script = this.$store.state.testRunner.testScripts[this.testId]
                })
            },
            loadTestReport() {
                this.report = this.$store.state.testRunner.testReports[this.testId]
            },
            actions(testIndex) {
                //console.log(`have ${this.script.test[testIndex].action.length} asserts`)
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

        },
        computed: {
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
            }
        },
        created() {
            this.loadTestScript()
            this.loadTestReport()
        },
        mounted() {

        },
        watch: {
            'testId': function() {
                this.loadTestScript()
                this.loadTestReport()
            }
        },
        mixins: [ errorHandlerMixin ],
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId', 'eventId'
        ],
        components: {
            //EvalReportAssert
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
.error {
    background-color: #0074D9 ;
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
