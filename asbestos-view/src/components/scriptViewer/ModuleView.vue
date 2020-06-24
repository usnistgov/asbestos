<template>
    <div>
    <div class="instruction title-box">
        <span class="big-bold">{{scriptOrModule}}</span>
        <span>{{label}}</span>
    </div>
    <div>
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
                <div v-if="isModule">
                    <div v-if="Object.getOwnPropertyNames(fixturesOut).length > 0" class="bold">Out</div>
                    <div v-else>None</div>
                    <div v-for="(fixtureName, fixtureName_i) in Object.getOwnPropertyNames(fixturesOut)" :key="'FixtureOut'+fixtureName_i">
                        <div class="selectable underline" @click="inNewTab(fixturesOut[fixtureName])">{{fixtureName}}</div>
                    </div>
                </div>
                <div v-else>
                    <div v-for="(fixture, fixture_i) in script.fixture" :key="'Fixture'+fixture_i">
                        <div class="soft-boxed">
                            <vue-json-pretty :data="fixture"></vue-json-pretty>
                        </div>
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
                <div class="container">
                    <div class="script bold">Script</div>
                    <div class="report bold">Report</div>
                </div>
                <div v-for="(setup, setup_i) in script.setup.action" :key="'Setup'+setup_i" class="container">
                    <div class="script soft-boxed">
                        <pretty-view
                            :data="setup"
                            :deep-view="deepSetupView['Setup'+setup_i]"> </pretty-view>
                    </div>

                    <span class="gully smaller">
                            <input type="checkbox" v-model="deepSetupView['Setup'+setup_i]">Expand
                    </span>

                    <div v-if="report &&
                                report.setup &&
                                report.setup.action &&
                                report.setup.action[setup_i]"
                         class="report soft-boxed" >
                        <pretty-view
                                :data="report.setup.action[setup_i]"
                                :deep-view="deepSetupView['Setup'+setup_i]"> </pretty-view>
                    </div>
                    <div v-else class="report soft-boxed">
                        <pretty-view
                            :data="null"
                            :deep-view="deepSetupView['Setup'+setup_i]"> </pretty-view>
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
                                :deep-view="deepTestView['Test'+test_i+'Action'+action_i]"
                                class="script"> </pretty-view>

                        <span class="gully smaller">
                            <input type="checkbox" v-model="deepTestView['Test'+test_i+'Action'+action_i]">Expand
                        </span>

                        <div class="report">
                            <div v-if="report &&
                                    report.test &&
                                    report.test[test_i] &&
                                    report.test[test_i].action &&
                                    report.test[test_i].action[action_i]">
                                <pretty-view
                                        :data="report.test[test_i].action[action_i]"
                                        :deep-view="deepTestView['Test'+test_i+'Action'+action_i]"> </pretty-view>

                            </div>
                            <div v-else>
                                <pretty-view
                                        :data="null"
                                        :deep-view="deepTestView['Test'+test_i+'Action'+action_i]"> </pretty-view>
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
                deepTestView: {},
                deepSetupView: {},
            }
        },
        computed: {
            scriptOrModule() {
                return this.isModule ? 'Module:  ' : 'Script:  ';
            },
            fixturesOut() {
                const fixtures = {};  // name => url
                if (!this.report.extension) {
                    return fixtures;
                }
                this.report.extension.forEach(function (ext) {
                    if (ext.url === 'urn:fixture-out') {
                        for (let i=0; i<ext.extension.length; i++) {
                            if (ext.extension[i].url && ext.extension[i].valueString)
                                fixtures[ext.extension[i].url] = ext.extension[i].valueString;
                        }
                    }
                })
                return fixtures;
            },
        },
        methods: {
            inNewTab(url) {
                console.log(`open ${url}`);
                window.open(url, "_blank");
            },
            toggleFixturesOpen() {
                this.fixturesOpen = !this.fixturesOpen;
            },
            toggleVariablesOpen() {
                this.variablesOpen = !this.variablesOpen;
            },
        },
        props: [
            'script', 'report', 'deepView', 'label', 'isModule'
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
        grid-template-columns: 45% 10% 45%;
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
    }
    .gully {
        grid-column: 2;
    }
    .report {
        grid-column: 3;
    }

</style>
