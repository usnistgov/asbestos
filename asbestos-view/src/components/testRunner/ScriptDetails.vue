<template>
    <div>
        <div v-if="script" class="script">
            <div v-if="script.description && script.description !== ''" class="script-description-margins">
                <vue-markdown>{{ description }}</vue-markdown>
            </div>
            <div v-else class="grayText">
                (No description.)
            </div>
            <div v-if="systemError" class="system-error">
                {{systemError}}
            </div>
            <div v-if="displayDetail">
                <div v-for="(fixture, i) in fixtures"
                     :key="i">
                    <span class="name">Fixture: </span>
                    <span class="value">{{ fixture.id }}</span>
                </div>
                <div v-for="(variable, i) in variables"
                     :key="'Var' + i">
                    <span class="name">Variable: </span>
                    <span class="value">{{ variable.name }}</span>
                </div>
            </div>

            <div v-if="script.setup">
                <ul class="noListStyle">
                    <debuggable-list-item
                            :key="'Setup0'"
                            :breakpoint-index="getBreakpointIndex('setup',0)"
                    >
                        <test-details
                                :script="script.setup"
                                :report="report ? report.setup : null"
                                :label="'Setup'"
                                :script-contained="script.contained"
                                :report-contained="report ? report.contained : null"
                                :test-type="'setup'"
                                :test-index="0"
                        ></test-details>
                    </debuggable-list-item>
                </ul>
            </div>
            <div v-if="script.test">
                <ul class="noListStyle">
                    <debuggable-list-item
                            v-for="(test, testi) in tests"
                            :key="'Test' + testi"
                            :breakpoint-index="getBreakpointIndex('test',testi)"
                    >
                        <test-details
                                :script="script.test[testi]"
                                :report="report && report.test  ? report.test[testi] : null"
                                :script-contained="script.contained"
                                :report-contained="report ? report.contained : null"
                                :test-type="'test'"
                                :test-index="testi"
                        ></test-details>
                    </debuggable-list-item>
                </ul>
            </div>

            <!-- add TEARDOWN here -->

        </div>
    </div>
</template>

<script>
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    import TestDetails from "./TestDetails";
    import DebuggableListItem from "./debugger/DebuggableListItem";
    import debugTestScriptMixin from "../../mixins/debugTestScript";
    import VueMarkdown from "vue-markdown";


    export default {
        data() {
            return {
                displayDetail: false,
            }
        },
        methods: {},
        computed: {
            systemError() {
                if (!this.report) return null;
                if (!this.report.extension) return null;
                let error = null
                this.report.extension.forEach(extension => {
                    if (extension.url === 'urn:failure')
                        error = extension.valueString;
                })
                return error
            },
            description() {
                return this.script.description.replace(/\\n\\n/g, "<br />").replace(/\\n/g, " ")
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
        },
        created() {
        },
        mounted() {

        },
        watch: {},
        mixins: [errorHandlerMixin, debugTestScriptMixin, ],
        props: [
            'script', 'report',  // TestScript and TestReport
            'testScriptIndex',
        ],
        components: {
            TestDetails,
            DebuggableListItem,
            VueMarkdown,
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
</style>
