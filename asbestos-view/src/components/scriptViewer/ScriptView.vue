<template>

    <div class="work-box">

        <input type="checkbox" v-model="deepView">Expand All
        <module-view
                :script="script"
                :report="report"
                :deep-view="deepView"
                :label="testCollection + '/' + testId"
                :header="reportHeader(report)"></module-view>

        <div v-if="moduleReports">
            <!--  script is modular and reports are available -->

            <div v-for="(mreport, mreport_i) in moduleReports"
                 :key="'Mreport'+mreport_i">

                <div v-if="mreport.name.startsWith(testId)">

                    <module-view
                        :script="moduleScripts[testId + '/' + moduleNameFromModularReport(mreport)]"
                        :report="mreport"
                        :deep-view="deepView"
                        :is-module="true"
                        :label="moduleIdFromModularReport(mreport)"
                        :header="reportHeader(mreport)"></module-view>
                </div>
            </div>
        </div>
        <div v-else-if="moduleScripts">
            <!-- script is modular but reports are not available -->

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
                deepView: false,
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
            moduleNameFromModularReport(report) {
                let name;
                const extensions = report.extension;
                for (let i=0; i<extensions.length; i++) {
                    const extension = extensions[i];
                    if (extension.url === 'urn:moduleName')
                        name = extension.valueString;
                }
                return name;
            },
            moduleIdFromModularReport(report) {
                let id;
                const extensions = report.extension;
                if (extensions) {
                    for (let i = 0; i < extensions.length; i++) {
                        const extension = extensions[i];
                        if (extension.url === 'urn:moduleId')
                            id = extension.valueString;
                    }
                }
                return id;
            },
            reportHeader(report) {
                if (!report)
                    return null;
                let header = {};
                if (report.extension)
                    header.extension = report.extension;
                if (report.modifierExtension)
                    header.modifierExtension = report.modifierExtension;
                if (report.name)
                    header.name = report.name;
                if (report.status)
                    header.status = report.status;
                if (report.testScript)
                    header.testScript = report.testScript;
                if (report.result)
                    header.result = report.result;
                if (report.issued)
                    header.issued = report.issued;
                return header;
            },
            moduleName(fullName) {
                  const parts = fullName.split('/');
                  if (parts.length > 1)
                      return parts[1];
                  return fullName;
            },
            moduleId(i) {
                const report = this.moduleReport(i);
                if (!report || !report.extension)
                    return null;
                for (let j=0; j<report.extension.length; j++) {
                    const extension = report.extension[j];
                    if (extension.url === 'urn:moduleId')
                        return extension.valueString;
                }
                return null;
            },
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

