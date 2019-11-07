<template>
    <div>
        <div v-if="script" class="script">
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
            <div v-if="script.setup && report.setup">
                <!-- don't need yet -->
            </div>

            <div v-if="!script.setup && report.setup">
                <test-report-action
                        :script="null"
                        :report="report.setup.action">
                </test-report-action>
            </div>


            <div v-for="(test, testi) in tests"
                 :key="'Test' + testi">
                <div v-if="report.test">
                    <template v-if="test.description" class="test-part">
                        SubTest: {{ test.description }}
                    </template>
                    Ref: {{ containedTests(testi) }}
                    Contained: {{ findContained(script, containedTests(testi)) }}
                    <div v-for="(action, actioni) in actions(testi)" class="test-part"
                         :key="'Test' + testi + 'Action' + actioni">
                        <test-report-action
                                :script="action"
                                :report="reportAction(testi, actioni)"> </test-report-action>
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

    export default {
        data() {
            return {
                script: null,
                report: null,
                displayDetail: false,
            }
        },
        methods: {
            containedTests(testi) {
                const mainTest = this.script.test[testi]
                if (mainTest)
                    console.log(`got mainTest`)
                const ref = this.containedTestReference(mainTest)
                return ref
            },
            containedTestReference(test) {
                if (!test.modifierExtension)
                    return null
                const ref = test.modifierExtension[0].valueReference.reference
                return ref.substring(1)
            },
            findContained(obj, id) {
                console.log(`findContained ${id}`)
                if (!obj.contained) {
                    console.log(`no contained`)
                    return
                }
                console.log(`has contained`)
                const cont = obj.contained[id]
                console.log(`cont is ${cont}`)
                console.log(`keys are ${Object.keys(obj.contained)}`)
                console.log(`key is ${obj.contained[0].id}`)
                return obj.contained[0]
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
                            console.info(`TestDetails: loaded test script ${this.testCollection}/${this.testId}`)
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
                console.log('grab test report')
                this.report = this.$store.state.testRunner.testReports[this.testId]
                //this.$router.go()
            },
            actions(testIndex) {
                return this.script.test[testIndex].action
            },
            scriptAction(testi, actioni) {
                return this.script.test[testi].action[actioni]
            },
            reportAction(testi, actioni) {
                console.log(`reportAction ${testi}   ${actioni}`)
                if (!this.report)
                    return null
                if (!this.report.test[testi])
                    return null
                return this.report.test[testi].action[actioni]
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
            TestReportAction
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
