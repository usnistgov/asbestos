<template>

    <div>
        <div v-if="script" class="script-display">

            <input type="checkbox" id="doEdit" v-model="edit">
            <label for="doEdit">Edit</label>
            <div class="divider"></div>

            <fixture-script
                    :fixtures="script.fixture"
                    :unused-fixtures="unusedFixtures"
                    :edit="edit"> </fixture-script>
            <variable-script
                    :variables="script.variable"
                    :unused-variables="unusedVariables"> </variable-script>
            <div class="setup-head">
                Setup
            </div>
            <div v-for="(setup, setupi) in script.setup"
                 :key="setup + setupi">
                <action-script :actions="setup"> </action-script>
            </div>
            <div v-for="(test, testi) in script.test"
                 :key="test + testi">

                <div class="test-test">Test</div>
                <div v-if="test.name">
                    <div class="test-name">name</div>
                    <div class="test-value">{{test.name}}</div>
                </div>
                <div v-if="test.description" class="test-box">
                    <div class="test-name">description</div>
                    <div class="test-value">{{test.description}}</div>
                </div>

                <action-script :actions="test.action"> </action-script>

            </div>

            <teardown-script :teardown="script.teardown"> </teardown-script>
        </div>
    </div>
</template>

<script>
    import ActionScript from "./ActionScript";
    import VariableScript from "./VariableScript";
    import FixtureScript from "./FixtureScript";
    import TeardownScript from "./TeardownScript";
    import unusedVariableScannerMixin from "../../mixins/unusedVariableScannerMixin";
    import unusedFixtureScannerMixin from "../../mixins/unusedFixtureScannerMixin";

    export default {
        data() {
            return {
                unusedVariables: [],
                unusedFixtures: [],
                edit: false,
            }
        },
        methods: {
        },
        computed: {
            },
        created() {
            if (this.script) {
                this.unusedVariables = this.scanForUnusedVariables(this.script)
                this.unusedFixtures = this.scanForUnusedFixtures(this.script)
            }
        },
        watch: {

        },
        name: "TestScript",
        props: [
            'script', 'report'
        ],
        mixins: [unusedVariableScannerMixin, unusedFixtureScannerMixin],
        components: {
            ActionScript, VariableScript, FixtureScript, TeardownScript
        }
    }
</script>

<style>
    .script-display {
        border: 3px solid;
    }
    .test-box {
        display: grid;
        grid-template-columns: 220px 700px;
    }
    .teardown-box {
        display: grid;
        grid-template-columns: 220px 700px;
    }
    .test-test {
        grid-column: 1;
        font-weight: bold;
        border-top: 1px solid;
        background: #4183c4;
    }
    .test-name {
        grid-column: 1;
    }
    .test-value {
        grid-column: 2;
    }
    .operation-box {
        display: grid;
        grid-template-columns: 20px 220px 700px;
    }
    .variable-box {
        display: grid;
        grid-template-columns: 220px 700px;
    }
    .fixture-box {
        display: grid;
        grid-template-columns: 220px 700px;
    }
    .teardown-box {
        display: grid;
        grid-template-columns: 220px 700px;
    }
    .variables-head {
        grid-column: 1;
        font-weight: bold;
        border-top: 1px solid;
        background: #aaaaaa;
    }
    .variable-name {
        grid-column: 1;
    }
    .variable-value {
        grid-column: 2;
    }
    .fixture-name {
        grid-column: 1;
    }
    .fixture-value {
        grid-column: 2;
    }
    .fixtures-head {
        grid-column: 1;
        font-weight: bold;
        border-top: 1px solid;
        background: #ffaaaa;
    }
    .setup-head {
        grid-column: 1;
        font-weight: bold;
        border-top: 1px solid;
        background: aquamarine;
    }
    .teardown-head {
        grid-column: 1;
        font-weight: bold;
        border-top: 1px solid;
        background: #ffaaaa;
    }
    .bold {
        font-weight: bold;
    }
    .top-border {
        border-top: 1px solid;
    }
    .spanner {
        grid-column-start: 1;
        grid-column-end: 3;
    }
    .name {
        grid-column: 2;
    }
    .operation-name {
        grid-column: 2;
        font-weight: bold;
        border-top: 1px solid;
        background: tan;
    }
    .assert-name {
        grid-column: 2;
        font-weight: bold;
        border-top: 1px solid;
    }
    .assert-value {
        grid-column: 3;
        font-weight: bold;
        border-top: 1px solid;
    }
    .value {
        grid-column: 3;
    }
    .operation-value {
        grid-column: 3;
        font-weight: bold;
        border-top: 1px solid;
        background: tan;
    }
    .red {
        color: red;
    }
    .left {
        text-align: left;
    }
    .edit-view {
        border: 2px solid;
        background: lightblue;
    }
</style>
