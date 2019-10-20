<template>
    <div>
        <div v-if="$store.state.testRunner.isClientTest">
            <client-details :sessionId="sessionId" :channelId="channelId" :testCollection="testCollection" :testId="testId"></client-details>
        </div>
        <div v-else>
            <test-details :sessionId="sessionId" :channelId="channelId" :testCollection="testCollection" :testId="testId"></test-details>
        </div>
    </div>
</template>

<script>
    import TestDetails from './TestDetails'
    import ClientDetails from './ClientDetails'
    export default {
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId'
        ],
        components: {
            TestDetails, ClientDetails
        },
        name: "TestOrEvalDetails"
    }
</script>

<style scoped>

</style>
