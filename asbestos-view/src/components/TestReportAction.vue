<template>
    <div v-bind:class="{pass : isPass, fail: isError, 'not-run': isNotRun}">
        <span v-if="this.script.operation" class="name selectable" @click="displayOperationMessage()">
            {{ this.operationType(this.script.operation) }}
        </span>
        <span v-else @click="displayAssertMessage()">
            <span class="name selectable">assert: </span>
            <span>{{ this.assertionDescription() }}</span>
        </span>
        <span class="selectable">
             {{ label }}
        </span>
        <div>{{ assertMessage }}</div>
        <div>{{ operationMessage }}</div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                operationMessage: null,
                assertMessage: null,
            }
        },
        methods: {
            operationType(operation) {
                return operation.type.code
            },
            assertionDescription() {
                return this.script.assert.description === undefined ? "" : this.script.assert.description
            },
            displayAssertMessage() {
                if (this.assertMessage)
                    this.assertMessage = null
                else
                    this.assertMessage = this.report.assert.message.replace('at ', '<br />at')
            },
            displayOperationMessage() {
                if (this.operationMessage)
                    this.operationMessage = null
                else
                    this.operationMessage = this.report.operation.message
            },
        },
        computed: {
            isPass() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                if (!part) return false
                return part.result !== 'error'
            },
            isError() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                if (!part) return false
                return part.result === 'error'
            },
            isNotRun() {
                return !this.report
            },
            operationOrAssertion() {
                return this.script.operation
                    ? `${this.operationType(this.script.operation)}`
                    : `Assert: ${this.assertionDescription()}`
            },

            label() {
                return this.script.operation ? this.script.operation.label : this.script.assert.label
            },
        },
        created() {

        },
        mounted() {

        },
        watch: {

        },
        props: [
            // parts representing a single action
            'script', 'report',
        ],
        name: "TestReportAction"
    }
</script>

<style scoped>
.name {
    font-weight: bold;
}
</style>
