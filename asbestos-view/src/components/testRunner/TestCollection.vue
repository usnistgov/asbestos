<template>
    <div>

        <test-collection-header
            :test-collection="testCollection"
            :session-id="sessionId"
            :channel-id="channelId"> </test-collection-header>

        <test-collection-body
                :test-collection="testCollection"
                :session-id="sessionId"
                :channel-id="channelId"> </test-collection-body>
    </div>
</template>

<script>
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import TestCollectionHeader from "./TestCollectionHeader";
    import TestCollectionBody from "./TestCollectionBody";
    import testCollectionMgmt from "../../mixins/testCollectionMgmt";

    export default {

        data() {
            return {
                time: [],   // built as a side-effect of status (computed)
            }
        },
        methods: {

            hasSuccessfulEvent(testId) {
                if (testId === null)
                    return false
                const eventResult = this.$store.state.testRunner.clientTestResult[testId]
                for (const eventId in eventResult) {
                    if (eventResult.hasOwnProperty(eventId)) {
                        const testReport = eventResult[eventId]
                        if (testReport.result === 'pass')
                            return  true
                    }
                }
                return false
            },

            testReport(testName) {
                if (!testName)
                    return null
                return this.$store.state.testRunner.testReports[testName]
            },
            load() {
                this.loadTestCollection(this.testCollection)
            },
        },
        computed: {
            //
            // testScriptNames() {
            //     const scripts = this.$store.state.testRunner.testScriptNames
            //     if (!scripts)
            //         return null
            //     const names = scripts.sort()
            //     return names
            // },
            // testReportNames() {  // just the ones with reports available
            //     const reports = this.$store.state.testRunner.testReports
            //     if (!reports)
            //         return null
            //     return Object.keys(reports).sort()
            // },

        },
        created() {
            // this.load(this.testCollection)
            // this.channel = this.channelId
            // this.setEvalCount()
        },
        mounted() {

        },
        watch: {
            // 'evalCount': 'setEvalCount',
            // 'testCollection': 'load',
            // 'channelId': function(newVal) {
            //     if (this.channel !== newVal)
            //         this.channel = newVal
            // },
        },
        mixins: [ errorHandlerMixin, colorizeTestReports, testCollectionMgmt ],
        name: "TestCollection",
        props: [
            'sessionId', 'channelId', 'testCollection',
        ],
        components: {
            TestCollectionHeader, TestCollectionBody,
        }
    }
</script>

<style scoped>
    .banner-color {
        background-color: lightgray;
        text-align: left;
    }
    .debugTestScriptButton {
        /*padding-bottom: 5px;*/
        margin-left: 10px;
        background-color: cornflowerblue;
        cursor: pointer;
        border-radius: 25px;
        font-weight: bold;
    }
    .configurationError {
        color: red;
    }
</style>
<style>
    .runallbutton {
        /*padding-bottom: 5px;*/
        background-color: cornflowerblue;
        cursor: pointer;
        border-radius: 25px;
        font-weight: bold;
    }
    .button-selected {
        border: 1px solid black;
        background-color: lightgray;
        cursor: pointer;
    }
    .button-not-selected {
        border: 1px solid black;
        cursor: pointer;
    }
    .runallgroup {
        text-align: right;
        padding-bottom: 5px;
    }
    .running {
        background-color: lightgreen;
    }
    .conformance-tests-header {
        background-color: #DBD9BE;
    }
    .noListStyle {
        list-style-type: none;
    }
    .pre-test-gap{
        height:1px;
        width:auto;
    }
    .large-text {
        font-size: large;
    }
    .pass {
        background-color: lightgreen;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .pass-plain {
        /*background-color: lightgray;*/
        text-align: left;
        border-top: 1px solid black;
        /*border-bottom: 1px solid black;*/
        cursor: pointer;
        /*border-radius: 25px;*/
    }
    .pass-plain-header {
        text-align: left;
        border-top: 1px solid black;
        cursor: pointer;
    }
    .pass-plain-detail {
        margin-bottom: 2px;
        text-align: left;
        cursor: pointer;
    }
    .fail {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .fail-plain {
        /*background-color: lightgray;*/
        text-align: left;
        border-top: 1px solid black;
        /*border-bottom: 1px solid black;*/
        cursor: pointer;
        /*border-radius: 25px;*/
    }
    .fail-plain-header {
        text-align: left;
        border-top: 1px solid black;
        cursor: pointer;
    }
    .fail-plain-detail {
        margin-bottom: 2px;
        text-align: left;
        cursor: pointer;
    }
    .condition-fail {
        background-color: gold;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .error-plain {
        /*background-color: cornflowerblue;*/
        text-align: left;
        border-top: 1px solid black;
        cursor: pointer;
        /*border-radius: 25px;*/
    }
    .error {
        background-color: cornflowerblue;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .not-run {
        background-color: lightgray;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .not-run-plain {
        text-align: left;
        border-top: 1px solid black;
        cursor: pointer;
    }
    .not-run-plain-detail {
        margin-bottom: 2px;
        text-align: left;
        cursor: pointer;
    }

    .align-right {
        text-align: right;
    }
    .align-left {
        text-align: left;
    }
    .breakpoint-indicator {
        list-style-type: "\1F6D1"; /* Stop sign */
    }
    .debug-hint {
        list-style-type: "\1F41E"; /* Lady bug */
    }
    .noTopMargin {
        margin-top: 0px;
    }
    .grayText {
        color: gray;
    }
    .testBarMargin {
        margin-bottom: 3px;
    }
    .inlineDiv {
        display: inline;
    }

</style>
