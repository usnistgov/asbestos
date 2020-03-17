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

            <div v-if="script.setup">
                <test-details
                        :script="script.setup"
                        :report="report ? report.setup : null"
                        :label="'Setup'"
                        :script-contained="script.contained"
                        :report-contained="report ? report.contained : null"
                ></test-details>
            </div>

            <div v-if="!script.setup">
                <action-details
                        :script="null"
                        :report="report && report.setup ? report.setup.action : null">
                </action-details>
            </div>

            <div v-if="script">
                <div v-for="(test, testi) in tests"
                     :key="'Test' + testi">
                    <test-details
                            :script="script.test[testi]"
                            :report="report ? report.test[testi] : null"
                            :script-contained="script.contained"
                            :report-contained="report ? report.contained : null"
                    ></test-details>

                </div>
            </div>

            <!-- add TEARDOWN here -->

        </div>
    </div>
</template>

<script>
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    import TestDetails from "./TestDetails";
    import ActionDetails from "./ActionDetails";

    export default {
        data() {
            return {
                displayDetail: false,
            }
        },
        methods: {
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
        },
        mounted() {

        },
        watch: {
        },
        mixins: [ errorHandlerMixin ],
        props: [
            'script', 'report'  // TestScript and TestReport
        ],
        components: {
            TestDetails, ActionDetails,
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
        margin-left: 50px;
        margin-right: 50px;
    }
    .conditional-margins {
        margin-left: 40px;
        margin-right: 40px;
    }
    .script-description-margins {
        margin-left: 30px;
        margin-right: 30px;
    }

</style>
