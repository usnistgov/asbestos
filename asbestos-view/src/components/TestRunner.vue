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
            <!--   SETUP   -->
            <div v-for="(test, testi) in tests"
                 :key="'Test' + testi">
                <span class="name selectable" >Test: </span>
                <span class="value selectable">{{ test.id }} </span>
                <span class="value selectable">{{ test.name }}</span>
                <div v-for="(action, actioni) in actions(testi)" class="test-part"
                     :key="'Test' + testi + 'Action' + actioni">
                    <span class="name selectable" >Action: </span>
                    <span class="value selectable">{{ operationOrAssertion(testi, actioni) }}{{ action.label }} </span>
                </div>
            </div>

            <!-- TEARDOWN -->
        </div>
    </div>
</template>

<script>
    import {ENGINE} from '../common/http-common'
    import errorHandlerMixin from '../mixins/errorHandlerMixin'

    export default {
        data() {
            return {
                testScripts: [],
                script: null,
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
                const that = this
                ENGINE.get(`collection/${this.testCollection}/${this.testId}`)
                    .then(response => {
                        console.info(`TestEnginePanel: loaded test script ${this.testCollection}/${this.testId}`)
                        this.testScripts.push( { name: this.testId, script: response.data })
                        this.script = response.data
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
            },
            actions(testIndex) {
                return this.script.test[testIndex].action
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
        },
        created() {
            this.loadTestScript()
        },
        mounted() {

        },
        watch: {

        },
        mixins: [ errorHandlerMixin ],
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId'
        ],
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
</style>
