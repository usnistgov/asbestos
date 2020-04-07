const path = require('path')

export default {
    data() {
        return {
            componentName: null,
        }
    },
    methods: {
        getComponentName(url) {
            const dirs = url.split(path.sep)
            if (dirs.length === 0) return url
            const nameAndExtension = dirs[dirs.length - 1]
            const parts = nameAndExtension.split('.')
            if (parts.length === 0) return nameAndExtension
            return parts[0]
        },
        // setImportComponentName(actionScript) {
        //     const theImport = this.getImportFromScript(actionScript)
        //     if (!theImport) return null
        //     let componentName = null
        //     if (!theImport.modifierExtension) return null
        //     theImport.modifierExtension.forEach(extension => {
        //         if (extension.url === 'component') {
        //             const value = extension.valueString
        //             componentName = this.getComponentName(value)
        //         }
        //     })
        //     if (componentName)
        //         this.componentName = this.currentTest + path.sep + componentName
        //     else
        //         this.componentName = null
        //     return this.componentName
        // },
        setImportComponentName(actionReport) {
            if (actionReport === null) return null;
            const moduleId = this.getModuleIdFromReport(actionReport);
            if (!moduleId) return null;
            this.componentName = this.currentTest + path.sep + moduleId;
            return this.componentName;
        },
        getImportFromScript(actionScript) {
            if (!actionScript.operation) return null
            if (!actionScript.operation.modifierExtension) return null
            let theImport = null
            actionScript.operation.modifierExtension.forEach(extension => {
                if (extension.url === 'https://github.com/usnistgov/asbestos/wiki/TestScript-Import')
                    theImport = extension
            })
            return theImport
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
