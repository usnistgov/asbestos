<template>
    <div>
        <div v-if="$store.state.testRunner.isClientTest">
            <eval-details :sessionId="sessionId" :channelId="channelId" :testCollection="testCollection" :testId="testId"></eval-details>
        </div>
        <div v-else>
            <test-details :sessionId="sessionId" :channelId="channelId" :testCollection="testCollection" :testId="testId"></test-details>
        </div>
    </div>
</template>

<script>
    import TestDetails from './TestDetails'
    import EvalDetails from './EvalDetails'
    export default {
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId'
        ],
        components: {
            TestDetails, EvalDetails
        },
        name: "TestOrEvalDetails"
    }
</script>

<style scoped>

</style>
