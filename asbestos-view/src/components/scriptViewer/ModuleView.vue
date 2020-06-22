<template>
    <div>
    <div class="instruction">
        <div class="big-bold">Script</div>
        <div>{{label}}</div>
    </div>
    <div>
<!--        <div class="script bold">Script</div>-->
<!--        <div class="report bold">Report</div>-->

        <div v-if="script">
            <div class="selectable" @click.stop="toggleFixturesOpen()">
                <span v-if="fixturesOpen" >
                    <img src="../../assets/arrow-down.png">
                </span>
                <span v-else>
                    <img src="../../assets/arrow-right.png"/>
                </span>
                <span class="big-bold">Fixtures</span>
            </div>
            <div v-if="fixturesOpen">
                <div v-for="(fixture, fixture_i) in script.fixture" :key="'Fixture'+fixture_i">
                    <div class="soft-boxed">
                        <vue-json-pretty :data="fixture"></vue-json-pretty>
                    </div>
                </div>
            </div>

            <div class="selectable"  @click.stop="toggleVariablesOpen()">
                <span v-if="variablesOpen">
                    <img src="../../assets/arrow-down.png">
                </span>
                <span v-else>
                    <img src="../../assets/arrow-right.png"/>
                </span>
                <span class="big-bold selectable">Variables</span>
            </div>
            <div v-if="variablesOpen">
                <div v-for="(variable, variable_i) in script.variable" :key="'Variable'+variable_i">
                    <div class="soft-boxed">
                        <vue-json-pretty :data="variable"></vue-json-pretty>
                    </div>
                </div>
            </div>

            <div v-if="script.setup">
                <div class="big-bold">Setup</div>
            </div>

            <div class="container">
                <div v-for="(setup, setup_i) in script.setup" :key="'Setup'+setup_i">
                    <div class="script bold">Script</div>
                    <div class="report bold">Report</div>
                    <div class="script soft-boxed">
                        <pretty-view
                            :data="setup"
                            :show-action="showAction"> </pretty-view>
                    </div>
                    <div v-if="report && report.setup && report.setup[setup_i]" class="report soft-boxed" >
                        <div class="report soft-boxed">
                            <pretty-view
                                :data="report.setup[setup_i]"
                                :show-action="showAction"> </pretty-view>
                        </div>
                    </div>
                    <div v-else class="soft-boxed">
                        <pretty-view
                            :data="null"
                            :show-action="showAction"> </pretty-view>
                    </div>
                </div>
            </div>

            <div v-for="(test, test_i) in script.test" :key="'Test'+test_i">
                <div class="big-bold">Test {{test_i}}</div>
                <div class="container">
                    <div class="script bold">Script</div>
                    <div class="report bold">Report</div>
                </div>
                    <div v-for="(action, action_i) in test.action" :key="'Test'+test_i+'Action'+action_i"
                    class="container">
                            <pretty-view
                                    :data="action"
                                    :show-action="showAction"
                                    class="script"> </pretty-view>

                        <div class="report">
                            <div v-if="report &&
                                    report.test &&
                                    report.test[test_i] &&
                                    report.test[test_i].action &&
                                    report.test[test_i].action[action_i]">
                                <pretty-view
                                        :data="report.test[test_i].action[action_i]"
                                        :show-action="showAction"> </pretty-view>

                            </div>
                            <div v-else>
                                <pretty-view
                                        :data="null"
                                        :show-action="showAction"> </pretty-view>
                            </div>
                        </div>
                    </div>

            </div>
        </div>
    </div>
    </div>
</template>

<script>
    import VueJsonPretty from 'vue-json-pretty'
    import PrettyView from "./PrettyView";
    export default {
        data() {
            return {
                fixturesOpen: false,
                variablesOpen: false,
            }
        },
        methods: {
            toggleFixturesOpen() {
                this.fixturesOpen = !this.fixturesOpen;
            },
            toggleVariablesOpen() {
                this.variablesOpen = !this.variablesOpen;
            },
        },
        props: [
            'script', 'report', 'showAction', 'label'
        ],
        components: {
            VueJsonPretty,
            PrettyView
        },
        name: "ModuleView"
    }
</script>

<style scoped>
    .container {
        display: grid;
        grid-template-columns: 50% 50%;
    }
    .script-header {
        grid-column: 1;
        grid-row: 1;
        font-weight: bold;
    }
    .report-header {
        grid-column: 2;
        grid-row: 1;
        font-weight: bold;
    }
    .script {
        grid-column: 1;
        /*grid-row: 2;*/
    }
    .report {
        grid-column: 2;
        /*grid-row: 2;*/
    }

</style>
