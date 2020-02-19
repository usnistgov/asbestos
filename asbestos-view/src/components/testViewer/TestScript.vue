<template>

    <div>
        <div v-if="script" class="script-display">
            <fixture-script :fixtures="script.fixture"> </fixture-script>
            <variable-script
                    :variables="script.variable"
                    :unused-variables="unusedVariables"> </variable-script>
            <div v-for="(setup, setupi) in script.setup"
                 :key="setup + setupi">
                <action-script :actions="setup"> </action-script>
            </div>
            <div v-for="(test, testi) in script.test"
                 :key="test + testi">

                <div class="test-test">Test</div>
                <div v-if="test.name">
                    <div class="test-name">name</div>
                    <div class="test-value">{{test.name}}</div>
                </div>
                <div v-if="test.description" class="test-box">
                    <div class="test-name">description</div>
                    <div class="test-value">{{test.description}}</div>
                </div>

                <action-script :actions="test.action"> </action-script>

            </div>

            <teardown-script :teardown="script.teardown"> </teardown-script>
        </div>
    </div>
</template>

<script>
    import ActionScript from "./ActionScript";
    import VariableScript from "./VariableScript";
    import FixtureScript from "./FixtureScript";
    import TeardownScript from "./TeardownScript";

    export default {
        data() {
            return {
                unusedVariables: []
            }
        },
        methods: {
            scanForUnusedVariables(script) {
                let unusedVariables = []
                const declaredVariables = this.scanScriptForDeclaredVariables(script)
                //console.log(`declared = ${declaredVariables}`)
                const referencedVariables = this.scanScriptForUsedVariables(script)
                console.log(`referenced = ${referencedVariables}`)
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
                    console.log(`for script.test`)
                    script.test.forEach(tst => {
                        console.log(`in`)
                        variables = variables.concat(this.scanActionsForVariables(tst.action))
                        console.log(`out`)
                    })
                    console.log(`back`)
                }
                if (script.teardown)
                    variables = variables.concat(this.scanActionsForVariables(script.teardown.action))
                // remove duplicates
                console.log(`scanScriptForUsedVariables(xxx) => ${variables}`)
                return variables.filter((a, b) => variables.indexOf(a) === b)
            },
            scanOperationForVariables(operation) {
                let variables = []
                variables = variables.concat(this.variableNamesFromString(operation.accept))
                variables = variables.concat(this.variableNamesFromString(operation.contentType))
                variables = variables.concat(this.variableNamesFromString(operation.method))
                variables = variables.concat(this.variableNamesFromString(operation.params))
                variables = variables.concat(this.variableNamesFromStrings(
                    operation.requestHeader.map(hdr => hdr.value)
                ))
                variables = variables.concat(this.variableNamesFromString(operation.url))
                console.log(`scanOperationForVariables(xxx) => ${variables}`)
                return variables
            },
            scanAssertForVariables(assert) {
                let variables = []
                variables = variables.concat(this.variableNamesFromString(assert.compareToSourceId))
                variables = variables.concat(this.variableNamesFromString(assert.compareToSourceExpression))
                variables = variables.concat(this.variableNamesFromString(assert.contentType))
                variables = variables.concat(this.variableNamesFromString(assert.expression))
                variables = variables.concat(this.variableNamesFromString(assert.requestURL))
                variables = variables.concat(this.variableNamesFromString(assert.responseCode))
                variables = variables.concat(this.variableNamesFromString(assert.value))
                console.log(`scanAssertForVariables(xxx) => ${variables}`)
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
                console.log(`scanActionsForVariables(xxx) => ${variables}`)
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
                console.log(`variableNamesFromString(${str}) => ${names}`)
                return names
            }
        },
        computed: {
            },
        created() {
            //console.log(`testing`)
            // console.log(`from String ${this.variableNamesFromString('Foo ${bar} x')}`)
            // console.log(`from Strings ${this.variableNamesFromStrings(['Foo ${bar} x', '${val}'])}`)
            //console.log(`multiple ${this.variableNamesFromString('${var}${foo}')}`)
            //console.log(`test ${this.scanAssertForVariables(this.script.test[0].action[6].assert)}`)
            if (this.script)
                this.unusedVariables = this.scanForUnusedVariables(this.script)
        },
        watch: {

        },
        name: "TestScript",
        props: [
            'script', 'report'
        ],
        components: {
            ActionScript, VariableScript, FixtureScript, TeardownScript
        }
    }
</script>

<style>
    .script-display {
        border: 3px solid;
    }
    .test-box {
        display: grid;
        grid-template-columns: 220px 700px;
    }
    .teardown-box {
        display: grid;
        grid-template-columns: 220px 700px;
    }
    .test-test {
        grid-column: 1;
        font-weight: bold;
        border-top: 1px solid;
        background: #445588;
    }
    .test-name {
        grid-column: 1;
    }
    .test-value {
        grid-column: 2;
    }
    .operation-box {
        display: grid;
        grid-template-columns: 20px 220px 700px;
    }
    .variable-box {
        display: grid;
        grid-template-columns: 220px 700px;
    }
    .fixture-box {
        display: grid;
        grid-template-columns: 220px 700px;
    }
    .teardown-box {
        display: grid;
        grid-template-columns: 220px 700px;
    }
    .variables-head {
        grid-column: 1;
        font-weight: bold;
        border-top: 1px solid;
        background: #aaaaaa;
    }
    .variable-name {
        grid-column: 1;
    }
    .variable-value {
        grid-column: 2;
    }
    .fixture-name {
        grid-column: 1;
    }
    .fixture-value {
        grid-column: 2;
    }
    .fixtures-head {
        grid-column: 1;
        font-weight: bold;
        border-top: 1px solid;
        background: #ffaaaa;
    }
    .teardown-head {
        grid-column: 1;
        font-weight: bold;
        border-top: 1px solid;
        background: #ffaaaa;
    }
    .bold {
        font-weight: bold;
    }
    .top-border {
        border-top: 1px solid;
    }
    .spanner {
        grid-column-start: 1;
        grid-column-end: 3;
    }
    .name {
        grid-column: 2;
    }
    .operation-name {
        grid-column: 2;
        font-weight: bold;
        border-top: 1px solid;
        background: tan;
    }
    .assert-name {
        grid-column: 2;
        font-weight: bold;
        border-top: 1px solid;
    }
    .assert-value {
        grid-column: 3;
        font-weight: bold;
        border-top: 1px solid;
    }
    .value {
        grid-column: 3;
    }
    .operation-value {
        grid-column: 3;
        font-weight: bold;
        border-top: 1px solid;
        background: tan;
    }
    .red {
        color: red;
    }
</style>
