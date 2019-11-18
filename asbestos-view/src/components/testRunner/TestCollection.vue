<template>
    <div>
        <div class="tool-title">
            <span>{{ clean(testCollection) }}</span>
            <span class="divider"></span>
            <img id="reload" class="selectable" @click="reload()" src="../../assets/reload.png"/>
            <span class="divider"></span>
        </div>

        <div class="vdivider"></div>
        <div class="vdivider"></div>

        <div class="left">
            Description: <span v-html="$store.state.testRunner.collectionDescription"></span>
        </div>

        <div class="vdivider"></div>
        <div class="vdivider"></div>

        <div class="instruction">
            <span v-if="$store.state.testRunner.isClientTest"  class="instruction">
                These are Client tests - the system under test sends messages to the
                FHIR Server Base Address shown below for evaluation. To run:
                <br />
                <ol>
                    <li>Send messages matching each per-test description.</li>
                    <li>Response message will reflect evaluation by the FHIR server (or XDS server for MHD tests) running in the background.</li>
                    <li>Once an adequate collection of messages has been sent to satisfy the tests, evaluate them further by clicking the
                    spyglass icon to evaluate against a set of assertions specific to each test.
                    This evaluation will include validating the response from the background server.</li>
                    <li>Only the most recent messages will be evaluated.  Adjust the count below.</li>
                    <li>A test passes if one or more message evaluates correctly.</li>
                    <li>Click on a test to see the messages evaluated.  Click on a message to see the result of each
                        assertion that was evaluated.</li>
                </ol>
                <div>
                    FHIR Server Base Address:
                    <span class="boxed">{{ clientBaseAddress }}</span>
                </div>
            </span>
            <span v-else  class="instruction">
                Server tests - click
                <img src="../../assets/press-play-button.png">
                to run test. <br />Requests will be sent to
                <span v-if="channelObj" class="boxed">{{ channelObj.fhirBase }}</span>
                <span v-if="channelObj">(Channel {{ channelObj.channelId }})</span>
            </span>
            <span class="divider"></span>
        </div>

        <div v-if="$store.state.testRunner.isClientTest" class="second-instruction">
            Number of most recent events to evaluate:
            <input v-model="evalCount" placeholder="5">
        </div>

        <div class="runallgroup">
            <button class="runallbutton" @click="doRunAll()">Run All</button>
        </div>

        <div v-for="(name, i) in Object.keys(status)"
             :key="name + i">
            <div >
                <div @click="selectTest(name)">
                    <div v-bind:class="{ pass: status[name] === 'pass', fail: status[name] === 'fail', error: status[name] === 'error', 'not-run': status[name] === 'not-run' }">
                        <div v-if="status[name] === 'pass'">
                            <img src="../../assets/checked.png" class="right">
                        </div>
                        <div v-else-if="status[name] === 'fail' || status[name] === 'error'">
                            <img src="../../assets/error.png" class="right">
                        </div>
                        <div v-else>
                            <img src="../../assets/blank-circle.png" class="right">
                        </div>
                        <div v-if="isClient">
                            <img src="../../assets/validate-search.png" class="right" @click.stop="doEval(name)">
                        </div>
                        <div v-else>
                            <img src="../../assets/press-play-button.png" class="right" @click.stop="doRun(name)">
                        </div>
                        Test: {{ clean(name) }}
                        <span v-if="!$store.state.testRunner.isClientTest"> --  {{ time[name] }}</span>
                    </div>
                </div>
                <div v-if="selected === name">
                    <router-view></router-view>
                </div>
            </div>
        </div>

    </div>
</template>

<script>
    import {ENGINE} from '../../common/http-common'
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'

    export default {

        data() {
            return {
                status: [],   // testId => undefined, 'pass', 'fail', 'error'
                time: [],
                evalCount: 5,
                channelObj: null,  // channel object
            }
        },
        methods: {
            clean(text) {
                return text.replace(/_/g, ' ')
            },
            evalStatus() { // see GetClientTestEvalResult
                this.status.splice(0)
                this.testScriptNames.forEach(testId => {
                    //console.log(`evalStatus ${testId}`)
                    const eventResult = this.$store.state.testRunner.clientTestResult[testId]
                    if (!eventResult) {
                        this.$set(this.status, testId, 'not-run')
                    } else if (this.hasSuccessfulEvent(testId)) {
                        this.$set(this.status, testId, 'pass')
                    } else {
                        this.$set(this.status, testId, 'fail')
                    }
                })
            },
            hasSuccessfulEvent(testId) {
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
            doEval(testName) {  // client tests
                //console.log(`doEval(${testName})`)
                this.$store.dispatch('runEval', testName)
            },
            doRun: async function(testName) {  // server tests
                this.$store.commit('setCurrentTest', null)
                try {
                    const response = await ENGINE.post(`testrun/${this.sessionId}__${this.channelId}/${this.testCollection}/${testName}`)
                    this.$store.commit('setTestReport', { testName: testName, testReport: response.data })
                    //this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`)
                    this.$store.dispatch('loadTestScriptNames')  // force reload of UI
                } catch (error) {
                    this.error(error)
                }
            },
            doRunAll() {
                this.status.length = 0
                Object.keys(this.status).forEach(name => {
                    this.doRun(name)
                })
            },
            selectTest(name) {
                if (this.selected === name)  { // unselect
                    this.$store.commit('setCurrentTest', null)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`
                    this.$router.push(route)
                    return
                }
                this.$store.commit('setCurrentTest', name)
                const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${name}`
                this.$router.push(route)
            },
            reload() {
                this.$store.commit('setTestCollectionName', this.testCollection)
                this.$store.dispatch('loadTestScriptNames')
                this.loadLastMarker()
                this.$store.dispatch('loadChannel', this.fullChannelId)
                    .then(channel => {
                        this.channelObj = channel
                    })
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`)
            },
            testReport(testName) {
                return this.$store.state.testRunner.testReports[testName]
            },
            updateReportStatuses() {   // for server tests
                //console.log('TestCollection: UpdateReportStatuses')
                let status = []
                let time = []
                this.testScriptNames.forEach(testName => {
                    if (this.testReport(testName) === undefined) {
                        status[testName] = 'not-run'
                    } else {
                        status[testName] = this.testReport(testName).result  // 'pass', 'fail', 'error'
                        time[testName] = this.testReport(testName).issued
                    }
                    this.status = status
                    this.time = time
                })

            },
            loadLastMarker() {
               if (this.$store.state.testRunner.lastMarker === null)
                    this.$store.dispatch('loadLastMarker')
            },
            setMarker() {
                this.$store.dispatch('setMarker')
            },
            testScriptNamesUpdated() {
                this.$store.dispatch('loadReports')
                //console.log(`client is ${this.isClient}`)
                if (this.isClient) {
                    //console.log(`its a client - names are ${this.$store.state.testRunner.testScriptNames}`)
                    return this.$store.state.testRunner.testScriptNames.forEach(name => {
                        //console.log(`Loading eval for ${name}`)
                        this.doEval(name)
                    })
                }
            },
            setEvalCount() {
                this.$store.commit('setEventEvalCount', this.evalCount)
            }
        },
        computed: {
            clientBaseAddress() { // for client tests
                return `${this.$store.state.testRunner.currentChannelBaseAddr}${this.sessionId}__${this.channelId}`
            },
            isClient() {
                return this.$store.state.testRunner.isClientTest
            },
            selected() {
                return this.$store.state.testRunner.currentTest
            },
            testScriptNames() {
                const scripts = this.$store.state.testRunner.testScriptNames
                const names = scripts.sort()
                //console.log(`script names = ${names}`)
                return names
            },
            testReportNames() {  // just the ones with reports available
                const reports = this.$store.state.testRunner.testReports
                return Object.keys(reports).sort()
            },
            channel: {
                set(name) {
                    if (name !== this.$store.state.base.channelId) {
                        this.$store.commit('setChannelId', name)
                    }
                },
                get() {
                    return this.$store.state.base.channelId
                }
            },
            fullChannelId() {
                return `${this.sessionId}__${this.channelId}`
            },
        },
        created() {
            this.reload()
            this.channel = this.channelId
            this.setEvalCount()
        },
        mounted() {

        },
        watch: {
            'evalCount': 'setEvalCount',
            'testCollection': 'reload',
            'channelId': function(newVal) {
                if (this.channel !== newVal)
                    this.channel = newVal
                this.loadLastMarker()
            },
            '$store.state.testRunner.testScriptNames' : 'testScriptNamesUpdated',
            '$store.state.testRunner.testReports': 'updateReportStatuses',
            '$store.state.testRunner.clientTestResult':'evalStatus'
        },
        mixins: [ errorHandlerMixin ],
        name: "TestCollection",
        props: [
            'sessionId', 'channelId', 'testCollection',
        ]
    }
</script>

<style scoped>
    .banner-color {
        background-color: lightgray;
        text-align: left;
    }
    .pass {
        background-color: lightgreen;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .fail {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .error {
        background-color: #0074D9 ;
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
    .right {
        text-align: right;
    }
    .runallbutton {
        /*padding-bottom: 5px;*/
        background-color: cornflowerblue;
        cursor: pointer;
        border-radius: 25px;
        font-weight: bold;
    }
    .runallgroup {
        text-align: right;
        padding-bottom: 5px;
    }
</style>
