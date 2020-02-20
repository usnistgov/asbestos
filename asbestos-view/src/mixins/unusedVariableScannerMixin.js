export default {
    methods: {
        scanForUnusedVariables(script) {
            let unusedVariables = []
            const declaredVariables = this.scanScriptForDeclaredVariables(script)
            const referencedVariables = this.scanScriptForUsedVariables(script)
            declaredVariables.forEach(variable => {
                if (!referencedVariables.includes(variable))
                    unusedVariables.push(variable)
            })
            return unusedVariables
        },
        scanScriptForDeclaredVariables(script) {
            return (script.variable) ? script.variable.map(v => v.name) : []
        },
        scanScriptForUsedVariables(script) {
            let variables = []
            if (script.setup)
                variables = variables.concat(this.scanActionsForVariables(script.setup.action))
            if (script.test) {
                script.test.forEach(tst => {
                    variables = variables.concat(this.scanActionsForVariables(tst.action))
                })
            }
            if (script.teardown)
                variables = variables.concat(this.scanActionsForVariables(script.teardown.action))
            // remove duplicates
            return variables.filter((a, b) => variables.indexOf(a) === b)
        },
        scanOperationForVariables(operation) {
            let variables = []
            if (!operation)
                return variables
            variables = variables.concat(this.variableNamesFromString(operation.accept))
            variables = variables.concat(this.variableNamesFromString(operation.contentType))
            variables = variables.concat(this.variableNamesFromString(operation.method))
            variables = variables.concat(this.variableNamesFromString(operation.params))
            if (operation.requestHeader)
                variables = variables.concat(this.variableNamesFromStrings(
                    operation.requestHeader.map(hdr => hdr.value)
                ))
            variables = variables.concat(this.variableNamesFromString(operation.url))
            return variables
        },
        scanAssertForVariables(assert) {
            let variables = []
            if (!assert)
                return variables
            variables = variables.concat(this.variableNamesFromString(assert.compareToSourceId))
            variables = variables.concat(this.variableNamesFromString(assert.compareToSourceExpression))
            variables = variables.concat(this.variableNamesFromString(assert.contentType))
            variables = variables.concat(this.variableNamesFromString(assert.expression))
            variables = variables.concat(this.variableNamesFromString(assert.requestURL))
            variables = variables.concat(this.variableNamesFromString(assert.responseCode))
            variables = variables.concat(this.variableNamesFromString(assert.value))
            return variables
        },
        scanActionsForVariables(actions) {
            let variables = []
            if (!actions)
                return variables
            actions.forEach(action => {
                variables = variables.concat(this.scanOperationForVariables(action.operation))
                variables = variables.concat(this.scanAssertForVariables(action.assert))
            })
            return variables
        },
        variableNameFromUsage(str, startingIndex) {  // startingIndex points to $ of ${xxxx}
            let open =  startingIndex + 1
            let close = str.indexOf("}", open + 1)
            if (close === -1)
                return null
            return str.substring(open+1, close)
        },
        variableNamesFromStrings(stringArray) {
            let names = []
            stringArray.forEach(str => names.push(this.variableNamesFromString(str)))
            return names
        },
        variableNamesFromString(str) {
            let names = []
            if (!str)
                return names
            let index = 0
            while (index !== -1) {
                index = str.indexOf("$", index)
                if (index === -1)
                    break
                const variable = this.variableNameFromUsage(str, index)
                if (variable === null)
                    break
                names.push(variable)
                index = index + 1
            }
            return names
        }

    }
}
