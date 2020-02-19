<template>
    <div>
        <div class="variables-head">Variables</div>
        <div v-for="(variable, variablei) in variables"
             :key="variable + variablei">

            <div v-if="variable.name" class="variable-box top-border">
                <div class="variable-name">name</div>
                <div class="variable-value">
                    {{variable.name}}
                    <span class="red">{{undefLabel(variable.name)}}</span>
                </div>
            </div>

            <div v-if="variable.description" class="variable-box">
                <div class="variable-name">description</div>
                <div class="variable-value">{{variable.description}}</div>
            </div>

            <div v-if="variable.defaultValue" class="variable-box">
                <div class="variable-name">defaultValue</div>
                <div class="variable-value">{{variable.defaultValue}}</div>
            </div>

            <div v-if="variable.expression" class="variable-box">
                <div class="variable-name">expression</div>
                <div class="variable-value">{{variable.expression}}</div>
            </div>

            <div v-if="variable.headerField" class="variable-box">
                <div class="variable-name">headerField</div>
                <div class="variable-value">{{variable.headerField}}</div>
            </div>

            <div v-if="variable.hint" class="variable-box">
                <div class="variable-name">hint</div>
                <div class="variable-value">{{variable.hint}}</div>
            </div>

            <div v-if="variable.path" class="variable-box">
                <div class="variable-name">path</div>
                <div class="variable-value">{{variable.path}}</div>
            </div>

            <div v-if="variable.sourceId" class="variable-box">
                <div class="variable-name">sourceId</div>
                <div class="variable-value">{{variable.sourceId}}</div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        methods: {
            undefLabel(variableName) {
                return this.unusedVariables.indexOf(variableName) === -1 ? "Unused" : ""
            }
        },
        name: "VariableScript",
        props: [
            'variables', 'unusedVariables'
        ]
    }
</script>

<style scoped>

</style>