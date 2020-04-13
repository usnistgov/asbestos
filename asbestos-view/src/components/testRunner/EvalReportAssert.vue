<template>
    <div>
<!--        assert pass/fail-->
        <div v-bind:class="[isAssertPass ? passClass : failClass, '']"  @click="toggleAssertMessageDisplay()">
            <span class="name selectable">assert: </span>
            <span>{{ assertionDescription }}</span>
        </div>

<!--        events listed per assert-->
        <div v-if="displayAssertMessage" class="event-part">
            <div v-for="(eventId, eventi) in eventIds" v-bind:class="[isEventPass(eventId) ? passClass : failClass, '']"
              :key="'Disp' + eventi">
                <div>
                    <eval-report-event
                            :event-id="eventId"
                            :test-report="eventResult[eventId]"
                            :action-index="actionIndex"></eval-report-event>
                </div>
            </div>
<!--            <pre>{{ message }}</pre>-->
        </div>
    </div>
</template>

<script>
    import EvalReportEvent from './EvalReportEvent'

    export default {
        data() {
            return {
                displayAssertMessage: false,
                status: [],   // testName => undefined, 'pass', 'fail', 'error'
                passClass: 'pass',
                failClass: 'fail',
            }
        },
        methods: {
            toggleAssertMessageDisplay() {  // displayOpen of the message returned in the assert
                this.displayAssertMessage = !this.displayAssertMessage
            },
        },
        computed: {
            assertDescription() {
                return this.assertScript.description
            },
            assertMessage() {
                return this.assertReport.message
            },
            assertScript() {
                return this.testScript.test[0].action[this.assertIndex].assert
            },
            assertResult() {  // pass, fail, warning, error
                return this.assertReport.result
            },
            assertLabel() {
                return this.assertScript.label
            },
            assertReport() {
                return this.testReport.test[0].action[this.assertIndex].assert
            },
            testReport() {
                return this.$store.state.testRunner.clientTestResult[this.testId][this.eventId]
            },
        },
        created() {

        },
        mounted() {

        },
        watch: {
        },
        props: [
            'sessionId', 'channelId', 'testCollection', 'testId', 'eventId', 'assertIndex'
        ],
        components: {
            EvalReportEvent,
        },
        name: "EvalReportAssert"
    }
</script>

<style scoped>
    .name {
        font-weight: bold;
    }
    .event-part {
        margin-left: 25px;
        margin-right: 25px;
    }
    .plain {

    }
</style>
