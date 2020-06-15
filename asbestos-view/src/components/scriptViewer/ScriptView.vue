<template>
    <div class="instruction">
        <div class="big-bold">Script</div>
        <div>{{testCollection}}/{{testId}}</div>
        <div class="vdivider"></div>
        <div class="vdivider"></div>

        <div v-if="script">
            <div class="big-bold">Fixtures</div>
            <div class="container" v-for="(fixture, fixture_i) in script.fixture" :key="'Fixture'+fixture_i">
                <div class="script soft-boxed">
                    <vue-json-pretty :data="fixture"></vue-json-pretty>
                </div>
            </div>

            <div class="big-bold">Variables</div>
            <div class="container" v-for="(variable, variable_i) in script.variable" :key="'Variable'+variable_i">
                <div class="script soft-boxed">
                    <vue-json-pretty :data="variable"></vue-json-pretty>
                </div>
            </div>

            <div class="big-bold">Setup</div>
            <div class="container" v-for="(setup, setup_i) in script.setup" :key="'Setup'+setup_i">
                <div class="script soft-boxed">
                    <vue-json-pretty :data="setup"></vue-json-pretty>
                </div>
                <div v-if="report && report.setup && report.setup[setup_i]" class="report soft-boxed" >
                    <vue-json-pretty :data="report.setup[setup_i]"></vue-json-pretty>
                </div>
                <div v-else class="soft-boxed">
                    <vue-json-pretty :data="null"></vue-json-pretty>
                </div>
            </div>

            <div class="big-bold">Tests</div>
            <div class="container" v-for="(test, test_i) in script.test" :key="'Test'+test_i">
                <div class="script soft-boxed">
                    <vue-json-pretty :data="test"></vue-json-pretty>
                </div>
                <div v-if="report && report.test && report.test[test_i]" class="report soft-boxed" >
                    <vue-json-pretty :data="report.test[test_i]"></vue-json-pretty>
                </div>
                <div v-else class="soft-boxed">
                    <vue-json-pretty :data="null"></vue-json-pretty>
                </div>
            </div>
            <div class="big-bold">Teardown</div>
        </div>

    </div>
</template>

<script>
    import VueJsonPretty from 'vue-json-pretty'

    export default {
        computed: {
            script() {
                this.loadTestScript();
                return this.$store.state.testRunner.testScripts[this.testId];
            },
            report() {
                this.loadTestReport();
                return this.$store.state.testRunner.testReports[this.testId];
            },
        },
        methods: {
            async loadTestScript() {
                if (this.$store.state.testRunner.testScripts[this.testId] === undefined) {
                    await this.$store.dispatch('loadTestScripts',
                       // this.testCollection,
                        [this.testId]
                    );
                }
            },
            async loadTestReport() {
                if (this.$store.state.testRunner.testReports[this.testId] === undefined)
                    await this.$store.dispatch('loadTestReports',
                        this.testCollection
                        //[this.testId]
                    );
            },
        },
        created() {
            this.$store.commit('setTestCollectionName', this.testCollection);
            this.$store.dispatch('loadCurrentTestCollection');
            this.loadTestScript();
            this.loadTestReport();
        },
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId'
        ],
        components: {
            VueJsonPretty
        },
        name: "ScriptView"
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
