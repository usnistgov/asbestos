const path = require('path')

export default {
    data() {
        return {
            // componentName: null,
        }
    },
    methods: {
        /*
        setComponentName(actionScript, actionReport) {
            if (actionReport === null) return null;
            const moduleId = this.getModuleIdFromReport(actionReport);
            if (!moduleId) return null;
            this.componentName = this.currentTest + path.sep + moduleId;
            return this.componentName;
        },

         */
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
        scriptImport(action) {
            if (!action.operation) return {'result' : {'hasImport' : false}}
            if (!action.operation.modifierExtension) return {'result' : {'hasImport' : false}}
            let hasImport = false
            let componentName = ''

            action.operation.modifierExtension.forEach(extension => {
                if (extension.url === 'https://github.com/usnistgov/asbestos/wiki/TestScript-Import') {
                    hasImport = true
                    if (extension.modifierExtension) {
                        extension.modifierExtension.forEach(extension => {
                            if (extension.url === 'component') {
                                let filePath = extension.valueString
                                let dotExtension = path.extname(filePath)
                                componentName = path.basename(filePath, dotExtension)
                            }
                        })
                    }

                }
            })
            let resultObj = {'result' :{'hasImport' : hasImport, 'componentName' : componentName}}
            return resultObj
        },
        // did call to module fail (different from action in module failing)
        reportContainsError(reportAction) {
            if (!reportAction)
                return false;
            if (reportAction.operation && reportAction.operation.result && reportAction.operation.result === 'error')
                return true;
            // I do not think this can happen.abbrev. Module call errors are reported in the operation.
            if (reportAction.assert && reportAction.assert.result && reportAction.assert.result === 'error')
                return true;
            return false;
        },

    },
    computed: {
        /*
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

         */
        currentTest() {
            return this.$store.state.testRunner.currentTest
        },
    },
}
