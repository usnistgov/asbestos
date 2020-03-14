<template>
    <div>
        <div v-if="script">
            <div v-bind:class="{
                'not-run': isNotRun,
                error: isError,
                pass : isPass,
                fail: isFail}"  class="test-margins" @click="toggleDisplay()">
                <span class="bold">Test: </span>{{ description }}
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                display: true,
            }
        },
        methods: {
            toggleDisplay() {
                this.Message = !this.Message
            },
        },
        computed: {
            description() {
                if (!this.script) return ""
                return this.script.description
            },
            isPass() {
                if (!this.report) return false
                this.report.action.forEach(action => {
                    const part = action.operation ? action.operation : action.assert
                    if (!part) return false
                    if (part.result !== 'pass') return false
                })
                return true
            },
            isError() {
                if (!this.report) return false
                this.report.action.forEach(action => {
                    const part = action.operation ? action.operation : action.assert
                    if (!part) return false
                    if (part.result === 'error') return true
                })
                return false
            },
            isFail() {
                if (!this.report) return false
                this.report.action.forEach(action => {
                    const part = action.operation ? action.operation : action.assert
                    if (!part) return false
                    if (part.result === 'fail') return true
                })
                return false
            },
            isNotRun() {
                if (!this.report) return true
                this.report.action.forEach(action => {
                    const part = action.operation ? action.operation : action.assert
                    if (!part) return true
                    if (part.result !== 'skip') return false
                })
                return false
            },
        },
        props: [
            // parts representing a single test element of a TestScript
            'script', 'report',
        ],
        name: "TestDetails"
    }
</script>

<style scoped>

</style>
