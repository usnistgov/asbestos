<template>
    <span>
            <span v-if="noReportStatus">
                <img src="../../assets/question-button.png" class="align-left">
            </span>
            <span v-else-if="fullScriptStatus === 'pass'">
                <img src="../../assets/checked.png" class="align-left">
            </span>
            <span v-else-if="fullScriptStatus === 'fail'">
                <img src="../../assets/error.png" class="align-left">
            </span>
            <span v-else-if="fullScriptStatus === 'error'">
                <img src="../../assets/yellow-error.png" class="align-left">
            </span>
            <span v-else>
                <img src="../../assets/blank-circle.png" class="align-left">
            </span>
    </span>
</template>

<script>
    export default {
        computed: {
            fullScriptStatus() {
                if (!this.eventId)  // server test
                    return this.$store.getters.testStatus[this.name];
                // client test
                const clientResults = this.$store.state.testRunner.clientTestResult;
                const testResults = clientResults[this.name];
                const clientResult = testResults[this.eventId][0];
                if (clientResult)
                    return clientResult.result;
                else
                    return null;
            },
            noReportStatus() {
                if (!this.eventId)  // server test
                    return this.$store.getters.testStatus.length === 0;
                // client test
                const clientResults = this.$store.state.testRunner.clientTestResult;
                if (clientResults === null) return true;
                const testResults = clientResults[this.name];
                if (testResults === null) return true;
                const clientResult = testResults[this.eventId][0];
                if (clientResult === null) return true;
                return false;
            }
        },
        props: [
            'statusRight',   // not used but some callers still pass it
            'name',          // testId
            'eventId'        // used if this is a client test
        ],
        name: "ScriptStatus"
    }
</script>

<style scoped>

</style>
