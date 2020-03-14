<template>
    <div>
        <div v-if="script" class="script">
            <div v-if="script.description" class="script-description-margins">
                {{ script.description }}
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

            <div v-if="script.setup && report && report.setup">
                <!-- don't need yet -->
            </div>

            <div v-if="!script.setup && report && report.setup">
                <action-details
                        :script="null"
                        :report="report.setup.action">
                </action-details>
            </div>


            <div v-for="(test, testi) in tests"
                 :key="'Test' + testi">
                <test-details
                        :script="script.test[testi]"
                        :report="report.test[testi]"
                ></test-details>


                <div v-for="(test2, test2i) in containedTests(findContained(script, containedTestsRef(script, testi)))"
                     :key="'Test2' + test2i">
                    <template v-if="test2.description" class="action-margins">
                        {{ test2.description }}
                    </template>
                    <div v-for="(action, actioni) in test2.action" class="action-margins"
                         :key="'Test' + testi + 'Action' + actioni">
                        <action-details
                                :script="action"
                                :report="findContained(report, containedTestsRef(report, testi)).test[test2i].action[actioni]"> </action-details>
                    </div>
                </div>
                <div v-for="(action, actioni) in actions(testi)" class="action-margins"
                     :key="'Test' + testi + 'Action' + actioni">
                    <action-details
                            :script="action"
                            :report="actionReport(report, testi, actioni)"> </action-details>
                </div>
            </div>

            <!-- add TEARDOWN here -->

        </div>
    </div>
</template>

<script>
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    import ActionDetails from './ActionDetails'
    import TestDetails from "./TestDetails";

    export default {
        data() {
            return {
                script: null,
                report: null,
                displayDetail: false,
            }
        },
        methods: {
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
                this.script = this.$store.dispatch('loadTestScript', {
                    testCollectionId: this.testCollection,
                    testId: this.testId
                })
            },
            loadTestReport() {
                this.report = this.$store.dispatch('loadTestReport', {
                    testCollectionId: this.testCollection,
                    testId: this.testId
                })
            },
            actions(testIndex) {
                return this.script.test[testIndex].action
            },
            scriptAction(testi, actioni) {
                return this.script.test[testi].action[actioni]
            },
            actionReport(report, testi, actioni) {
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
            this.script = this.$store.state.testRunner.testScripts[this.$store.state.testRunner.currentTest]
            this.report = this.$store.state.testRunner.testReports[this.$store.state.testRunner.currentTest]
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
            ActionDetails, TestDetails,
        },
        name: "ScriptDetails"
    }
</script>

<style scoped>
.script {
    text-align: left;
}
    .name {
        font-weight: bold;
    }
    .value {

    }

</style>
<style>
    .test-margins {
        margin-left: 20px;
        margin-right: 20px;
    }
    .action-margins {
        margin-left: 40px;
        margin-right: 40px;
    }
    .script-description-margins {
        margin-left: 30px;
        margin-right: 30px;
    }

</style>
