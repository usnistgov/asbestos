<template>
    <div>
        <div v-if="$store.state.testRunner.isClientTest">
            <client-details :sessionId="sessionId" :channelId="channelId" :testCollection="testCollection" :testId="testId"></client-details>
        </div>
        <div v-else>
            <script-details
                :script="$store.state.testRunner.testScripts[$store.state.testRunner.currentTest]"
                :report="$store.state.testRunner.testReports[$store.state.testRunner.currentTest]"
            > </script-details>
        </div>
    </div>
</template>

<script>
    import ScriptDetails from './ScriptDetails'
    import ClientDetails from './ClientDetails'
    export default {
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId'
        ],
        components: {
            ScriptDetails, ClientDetails
        },
        mounted() {
            if (this.$store.state.testRunner.testAssertions === null)
                this.$store.dispatch('loadTestAssertions')
        },
        name: "TestOrEvalDetails"
    }
</script>

<style scoped>

</style>
