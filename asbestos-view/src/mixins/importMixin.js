const path = require('path')

export default {
    data() {
        return {
            componentName: null,
        }
    },
    methods: {
        setComponentName(actionReport) {
            if (actionReport === null) return null;
            const moduleId = this.getModuleIdFromReport(actionReport);
            if (!moduleId) return null;
            this.componentName = this.currentTest + path.sep + moduleId;
            return this.componentName;
        },
        getModuleIdFromReport(actionReport) {
            if (!actionReport) return null;
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
            if (!script || !script.test || !script.test[0]) return null
            return script.test[0].action
        },
        componentReportActions() {
            if (this.componentName === null) return null
            const report = this.$store.state.testRunner.moduleTestReports[this.componentName]
            if (!report || !report.test || !report.test[0]) return null
            return report.test[0].action
        },
        currentTest() {
            return this.$store.state.testRunner.currentTest
        },
    },
}
