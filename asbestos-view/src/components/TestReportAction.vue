<template>
    <div>
        <span class="name selectable" >Action: </span>
        <span v-bind:class="{pass : isPass, fail: isError, 'not-run': isNotRun}">
            {{ operationOrAssertion }}{{ label }}
        </span>
    </div>
</template>

<script>
    export default {
        data() {
            return {
            }
        },
        methods: {
            operationType(operation) {
                return operation.type.code
            },
        },
        computed: {
            isPass() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                return part.result !== 'error'
            },
            isError() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                return part.result === 'error'
            },
            isNotRun() {
                return !this.report
            },
            operationOrAssertion() {
                return this.report.operation ? `Operation: ${this.operationType(this.script.operation)}` : `Assert: ${this.assertionDescription(this.report.assert)}`
            },
            assertionDescription() {
                return this.report.description === undefined ? "" : this.report.description
            },
            label() {
                return this.script.operation ? this.script.operation.label : this.script.assert.label
            }
        },
        created() {

        },
        mounted() {

        },
        watch: {

        },
        props: [
            // just that action parts
            'script', 'report',
        ],
        name: "TestReportAction"
    }
</script>

<style scoped>

</style>
