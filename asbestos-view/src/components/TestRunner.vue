<template>
    <div>
        <div v-if="script" class="script">
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
            <!--   add SETUP   -->
            <div v-for="(test, testi) in tests"
                 :key="'Test' + testi">
                <span class="name" >Test: </span>
                <span class="value">{{ test.id }} </span>
                <span class="value">{{ test.name }}</span>
                <div v-for="(action, actioni) in actions(testi)" class="test-part"
                     :key="'Test' + testi + 'Action' + actioni">
                    <test-report-action :script="scriptAction(testi, actioni)" :report="reportAction(testi, actioni)"></test-report-action>
<!--                    <span class="name selectable" >Action: </span>-->
<!--                    <span v-bind:class="{pass : isPass(testi, actioni), fail: isError(testi, actioni), 'not-run': notRun(testi, actioni)}">{{ operationOrAssertion(testi, actioni) }}{{ action.label }} </span>-->
                </div>
            </div>
            <!-- add TEARDOWN -->
        </div>
    </div>
</template>

<script>
    import {ENGINE} from '../common/http-common'
    import errorHandlerMixin from '../mixins/errorHandlerMixin'
    import TestReportAction from './TestReportAction'

    export default {
        data() {
            return {
                script: null,
                report: null,
            }
        },
        methods: {
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
                if (this.$store.state.base.testScripts[this.testId] === undefined) {
                    const that = this
                    ENGINE.get(`collection/${this.testCollection}/${this.testId}`)
                        .then(response => {
                            console.info(`TestEnginePanel: loaded test script ${this.testCollection}/${this.testId}`)
                            this.$store.commit('addTestScript', {name: this.testId, script: response.data})
                            this.script = response.data
                        })
                        .catch(function (error) {
                            that.error(error)
                        })
                }
            },
            loadTestReport() {  // loaded by TestList
                this.report = this.$store.state.base.testReports[this.testId]
            },
            actions(testIndex) {
                return this.script.test[testIndex].action
            },
            scriptAction(testi, actioni) {
                return this.script.test[testi].action[actioni]
            },
            reportAction(testi, actioni) {
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

        },
        mixins: [ errorHandlerMixin ],
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId'
        ],
        components: {
            TestReportAction
        },
        name: "TestRunner"
    }
</script>

<style scoped>
.script {
    text-align: left;
}
.test-part {
    margin-left: 10px;
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
}
.fail {
    background-color: indianred;
    text-align: left;
    border: 1px dotted black;
    cursor: pointer;
}
.not-run {
    background-color: lightgray;
    text-align: left;
    border: 1px dotted black;
    cursor: pointer;
}
</style>
