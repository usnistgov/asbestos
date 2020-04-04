export default {
    data() {
        return {
            componentName: null,
        }
    },
    methods: {
        getComponentName(url) {
            const dirs = url.split('/')
            if (dirs.length === 0) return url
            const nameAndExtension = dirs[dirs.length - 1]
            const parts = nameAndExtension.split('.')
            if (parts.length === 0) return nameAndExtension
            return parts[0]
        },
        setImportComponentName(actionScript) {
            const theImport = this.getImport(actionScript)
            if (!theImport) return null
            console.log(`import`)
            let componentName = null
            if (!theImport.modifierExtension) return null
            console.log(`import.modifierExtension`)
            theImport.modifierExtension.forEach(extension => {
                if (extension.url === 'component') {
                    const value = extension.valueString
                    componentName = this.getComponentName(value)
                }
            })
            console.log(`componentName ${componentName}`)
            if (componentName)
                this.componentName = this.currentTest + '/' + componentName
            else
                this.componentName = null
            console.log(`componentName ${this.componentName}`)
            return this.componentName
        },
        getImport(actionScript) {
            if (!actionScript.operation) return null
            if (!actionScript.operation.modifierExtension) return null
            let theImport = null
            actionScript.operation.modifierExtension.forEach(extension => {
                if (extension.url === 'https://github.com/usnistgov/asbestos/wiki/TestScript-Import')
                    theImport = extension
            })
            return theImport
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
