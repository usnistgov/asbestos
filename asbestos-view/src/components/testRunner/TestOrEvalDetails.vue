<template>
    <ul class="noTopMargin">
        <li v-if="$store.state.testRunner.isClientTest && testScript">
                <div class="instruction">
                    {{ testScript.description }}
                </div>
                <div v-if="eventIds === null">
                    No messages present on this channel
                </div>
                <div v-else>
                    <ul class="noListStyle">
                    <li v-for="(eventId, eventi) in eventIds"
                         :key="'Disp' + eventi">
                        <client-details
                                :sessionId="sessionId"
                                :channelId="channelId"
                                :testCollection="testCollection"
                                :testId="testId"
                                :eventId="eventId"></client-details>
                    </li>
                    </ul>
                </div>
        </li>
        <li v-else>
            <script-details
                :script="$store.state.testRunner.testScripts[$store.state.testRunner.currentTest]"
                :report="$store.state.testRunner.testReports[$store.state.testRunner.currentTest]"
            > </script-details>
        </li>
    </ul>
</template>

<script>
    import ScriptDetails from './ScriptDetails'
    import ClientDetails from './ClientDetails'
    export default {
        computed: {
            testScript() {
                return   this.$store.state.testRunner.testScripts[this.testId]
            },
            eventIds() {
                if (!this.$store.state.testRunner.clientTestResult) return null
                return Object.keys(this.$store.state.testRunner.clientTestResult[this.testId])
            },
        },
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
