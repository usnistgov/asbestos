<template>
    <div>
    <ul class="noTopMargin">
        <li v-if="$store.state.testRunner.isClientTest">
            <div v-if="testScript">
                <div class="instruction">
                    <vue-markdown>{{ testScript.description }}</vue-markdown>
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
            </div>
        </li>
        <li v-else>
            <script-details
                :script="$store.state.testRunner.testScripts[$store.state.testRunner.currentTest]"
                :report="$store.state.testRunner.testReports[$store.state.testRunner.currentTest]"
                :test-script-index="$store.state.testRunner.serverTestCollectionNames.indexOf(testCollection) + '.' + $store.state.testRunner.testScriptNames.indexOf(testId)"
            > </script-details>
        </li>
    </ul>
    </div>
</template>

<script>
    import ScriptDetails from './ScriptDetails'
    import ClientDetails from './ClientDetails'
    import VueMarkdown from "vue-markdown";
    export default {
        computed: {
            description() {
                if (!this.$store.state.testRunner.testScripts) return null
                if (!this.$store.state.testRunner.testScripts[this.testId].description) return null
                return this.$store.state.testRunner.testScripts[this.testId].description.replace(/\n/g, "<br />")
            },
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
            ScriptDetails, ClientDetails, VueMarkdown,
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
