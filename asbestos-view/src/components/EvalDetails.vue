<template>
    <div>
        <div v-if="script" class="script">
            <div v-if="script.description">
                {{ script.description }}
            </div>
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
                    <div @click="selectAssert(actioni)">
                        Assert: {{ assertScript(actioni).description }}
                        <div v-if="selectedAssert === actioni">
                            Message: {{ assertMessage(actioni) }}
                        </div>
                    </div>
                </div>
            </div>

            <!-- add TEARDOWN here -->

        </div>
    </div>
</template>

<script>
    import {ENGINE} from '../common/http-common'
    import errorHandlerMixin from '../mixins/errorHandlerMixin'
    //import EvalReportAssert from './EvalReportAssert'

    export default {
        data() {
            return {
                script: null,
                report: null,
                selectedAssert: null,
            }
        },
        methods: {
            assertMessage(assertIndex) {
                console.log(`in assertMessage(${assertIndex})`)
                return `Hi ${assertIndex}` //this.assertReport(assertIndex).message
            },
            assertReport(assertIndex) {
                return this.testReport.test[0].action[assertIndex].assert
            },
            assertScript(assertIndex) {
                return this.testScript.test[0].action[assertIndex].assert
            },
            selectAssert(assertIndex) {
                console.log(`assertIndex is ${assertIndex}`)
                console.log(`selectedAssert is ${this.selectedAssert}`)
                this.selectedAssert = assertIndex
                // if (this.selected === assertIndex)  { // unselect
                //     this.$store.commit('setCurrentAssertIndex', null)
                //     const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${this.testId}/event/${this.eventId}`
                //     this.$router.push(route)
                // } else {
                //     this.$store.commit('setCurrentAssertIndex', assertIndex)
                //     const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${this.testId}/event/${this.eventId}/assert/${assertIndex}`
                //     this.$router.push(route)
                // }
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
                if (this.$store.state.testRunner.testScripts[this.testId] === undefined) {
                    console.info(`${this.testId} needed loading`)
                    const that = this
                    ENGINE.get(`collection/${this.testCollection}/${this.testId}`)
                        .then(response => {
                            console.info(`EvalDetails: loaded test script ${this.testCollection}/${this.testId}`)
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
            loadTestReport() {  // loaded by TestList
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
            'testId': 'loadTestReport'
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
