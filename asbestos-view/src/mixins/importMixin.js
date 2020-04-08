const path = require('path')

export default {
    data() {
        return {
            componentName: null,
        }
    },
    methods: {
        setImportComponentName(actionReport) {
            if (actionReport === null) return null;
            const moduleId = this.getModuleIdFromReport(actionReport);
            if (!moduleId) return null;
            this.componentName = this.currentTest + path.sep + moduleId;
            return this.componentName;
        },
        getModuleIdFromReport(actionReport) {
            if (!actionReport.operation) return null;
            if (!actionReport.operation.extension) return null;
            let moduleId = null;
            actionReport.operation.extension.forEach(extension => {
                if (extension.url === 'urn:moduleId')
                    moduleId = extension.valueString;
            })
            return moduleId;
        },
    },
    computed: {
        componentScriptActions() {
            if (this.componentName === null) return null
            const script = this.$store.state.testRunner.moduleTestScripts[this.componentName]
            return script.test[0].action
        },
        componentReportActions() {
            if (this.componentName === null) return null
            const report = this.$store.state.testRunner.moduleTestReports[this.componentName]
            return report.test[0].action
        },
        currentTest() {
            return this.$store.state.testRunner.currentTest
        },
    },
}
