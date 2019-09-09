<template>
    <div>
        <div v-bind:class="{'not-run': isNotRun, pass : isPass, fail: isError}"  @click="displayMessage()">
            <span v-if="this.script.operation" class="name selectable">
                {{ this.operationType(this.script.operation) }}
            </span>
            <span v-else>
                <span class="name selectable">assert: </span>
                <span>{{ this.assertionDescription() }}</span>
            </span>
            <span class="selectable">
                {{ label }}
            </span>
        </div>
        <div v-if="message"><pre>{{ message }}</pre></div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                message: null,
            }
        },
        methods: {
            operationType(operation) {
                return operation.type.code
            },
            assertionDescription() {
                return this.script.assert.description === undefined ? "" : this.script.assert.description
            },
            displayMessage() {
                if (this.message)
                    this.message = null
                else if (this.report)
                    this.message = this.report.assert
                        ? this.report.assert.message
                        : this.report.operation.message
            },
        },
        computed: {
            isPass() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                if (!part) return false
                return part.result !== 'error' && part.result !== 'fail'
            },
            isError() {
                if (!this.report) return false
                const part = this.report.operation ? this.report.operation : this.report.assert
                if (!part) return false
                return part.result === 'error' || part.result === 'fail'
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
