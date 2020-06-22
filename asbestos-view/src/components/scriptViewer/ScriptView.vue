<template>

    <div class="work-box">

        <input type="checkbox" v-model="showAction">Show Action Wrappers
        <div>
            <module-view
                :script="script"
                :report="report"
                :show-action="showAction"
                :label="testCollection + '/' + testId"></module-view>

            <div class="big-bold">Modules</div>

            <div v-for="(module, module_i) in moduleScripts"
                 :key="'Module'+module_i">
                <module-view
                    :script="module"
                    :report="moduleReport(module_i)"
                    :show-action="showAction"
                    :label="testCollection + '/' + testId"></module-view>
            </div>
        </div>
    </div>
</template>

<script>
    import ModuleView from "./ModuleView";

    export default {
        data() {
            return {
                fixturesOpen: false,
                variablesOpen: false,
                showAction: false,
            }
        },
        computed: {
            script() {
                this.loadTestScript();
                return this.$store.state.testRunner.testScripts[this.testId];
            },
            report() {
                this.loadTestReport();
                return this.$store.state.testRunner.testReports[this.testId];
            },
            moduleScripts() {
                return this.$store.state.testRunner.moduleTestScripts;
            },
            moduleReports() {
                return this.$store.state.testRunner.moduleTestReports;
            }
        },
        methods: {
            moduleReport(i) {
                if (this.moduleReports && this.moduleReports[i])
                    return this.moduleReports[i];
                return null;
            },
            toggleFixturesOpen() {
                this.fixturesOpen = !this.fixturesOpen;
            },
            toggleVariablesOpen() {
                this.variablesOpen = !this.variablesOpen;
            },
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
            ModuleView
        },
        name: "ScriptView"
    }
</script>

<style scoped>

</style>

