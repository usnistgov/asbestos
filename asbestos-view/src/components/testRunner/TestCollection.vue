<template>
    <div>
        <div class="tool-title">
            <span>{{ clean(testCollection) }}</span>
            <span class="divider"></span>
            <img id="reload" class="selectable" @click="load()" src="../../assets/reload.png"/>
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
                    Send to:
                    <span class="boxed">{{ clientBaseAddress }}</span>  based on the Channel selection.
                </div>
            </span>
            <span v-else  class="instruction">
                Server tests - click
                <img src="../../assets/press-play-button.png">
                to run test.
                <!-- Display the property, if it exists, based on channel Type -->
                <div v-if="channelObj">
                    <span v-if="channelObj.channelType === 'passthrough' || channelObj.channelType === 'fhir'">
                        Requests will be sent to
                        <span v-if="channelObj.fhirBase" class="boxed">{{ channelObj.fhirBase }}</span>
                        <div class="divider"></div>
                        (through the Proxy on Channel {{ channelObj.channelId }}) based on the Channel selection.
                    </span>
                    <span v-else-if="channelObj.channelType === 'mhd'">
                         Requests will be sent to XDS Site:
                        <span v-if="channelObj.xdsSiteName" class="boxed">{{ channelObj.xdsSiteName }}</span>
                        <div class="divider"></div>
                        (through the Proxy on Channel {{ channelObj.channelId }}) based on the Channel selection.
                    </span>
                    <span v-else class="configurationError">
                        Unknown channel.channelType for {{channelObj.channelId }}.
                    </span>
                </div>
            </span>
            <span class="divider"></span>
        </div>

        <div v-if="$store.state.testRunner.isClientTest" class="second-instruction">
            Number of most recent events to evaluate:
            <input v-model="evalCount" placeholder="5">
        </div>

        <div class="runallgroup">
            <span v-if="running" class="running">Running</span>
            <div class="divider"></div>
            <div class="divider"></div>

            <span v-if="!$store.state.testRunner.isClientTest">
                <input type="checkbox" id="doGzip" v-model="gzip">
                <label for="doGzip">GZip?</label>
                <div class="divider"></div>
            </span>

            <button v-bind:class="{'button-selected': useJson, 'button-not-selected': !useJson}" @click="doJson()">JSON</button>
            <button v-bind:class="{'button-selected': !useJson, 'button-not-selected': useJson}" @click="doXml()">XML</button>
            <div class="divider"></div>
            <div class="divider"></div>
            <button class="runallbutton" @click="doRunAll()">Run All</button>
        </div>

        <div v-if="status">
<!--            Object.keys(status)"-->
            <div v-for="(name, i) in scriptNames"
                 :key="name + i">
                <div >
                    <div @click="openTest(name)">
                        <div v-bind:class="{
                            pass: status[name] === 'pass',
                            fail: status[name] === 'fail',
                            error: status[name] === 'error',
                            'not-run': status[name] === 'not-run' }">
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

                            Script: {{ clean(name) }}
                            <span v-if="!$store.state.testRunner.isClientTest"> --  {{ time[name] }}</span>
                        </div>
                    </div>
                    <div v-if="selected === name">
                        <router-view></router-view>
                    </div>
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
                time: [],   // built as a side-effect of status (computed)
                evalCount: 5,
                channelObj: null,  // channel object
                useJson: true,
                running: false,
                gzip: false,
            }
        },
        methods: {
            doJson() {
                this.useJson = true
            },
            doXml() {
                this.useJson = false
            },
            clean(text) {
                if (text)
                    return text.replace(/_/g, ' ')
                return ''
            },
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
            async doEval(testName) {  // client tests
                if (testName)
                    await this.$store.dispatch('runEval', testName)
                // ==> commits clientTestResult
                //    ==> calls evalStatus
            },
            async runner(testName) {
                if (!testName)
                    return
                this.$store.commit('setCurrentTest', null)
                try {
                    const response = await ENGINE.post(`testrun/${this.sessionId}__${this.channelId}/${this.testCollection}/${testName}?_format=${this.useJson ? 'json' : 'xml'};_gzip=${this.gzip}`)
                    const report = response.report
                    this.$store.commit('setTestReport', report)
                } catch (error) {
                    this.error(error)
                }
            },
            async doRun(testName) {  // server tests
                if (!testName)
                    return
                this.running = true
                await this.$store.dispatch('runTest', testName)
                this.running = false
            },
            async doRunAll()  {
                if (!this.status)
                    return
                this.running = true
                //for (const name of Object.keys(this.status)) {
                for (const name of this.$store.state.testRunner.testScriptNames) {
//                    console.log(`runAll test ${name}`)
                    if (this.isClient)
                        await this.doEval(name)
                    else
                        await this.runner(name)
                    //await this.runner(name)
                }
                this.running = false
                await this.$store.dispatch('loadCurrentTestCollection')  // force load of UI
            },
            openTest(name) {
                if (!name)
                    return
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
            async load() {
                this.$store.commit('setTestCollectionName', this.testCollection)
                await this.$store.dispatch('loadCurrentTestCollection')
                this.testScriptNamesUpdated()
                const requiredChannel = this.$store.state.testRunner.requiredChannel
                if (requiredChannel) {
                    this.$store.commit('setChannelId', requiredChannel)
                }
                this.$store.dispatch('loadChannel', this.fullChannelId)
                    .then(channel => {
                        this.channelObj = channel
                    })
                // these are ok left as async - don't need an await
                this.$store.dispatch('loadTestScripts', this.$store.state.testRunner.testScriptNames)
                this.$store.dispatch('loadTestReports', this.$store.state.testRunner.currentTestCollectionName)
            },
            testReport(testName) {
                if (!testName)
                    return null
                return this.$store.state.testRunner.testReports[testName]
            },
            importStatusForServerTests() {
                let status = []
                let time = []
                this.testScriptNames.forEach(testId => {
                    const testReport = this.$store.state.testRunner.testReports[testId]
                    if (testReport === undefined) {
                        status[testId] = 'not-run'
                    } else {
                        status[testId] = testReport.result  // 'pass', 'fail', 'error'
                        time[testId] = testReport.issued
                    }
                })
                this.time = time
                return status
            },
            importStatusClientTests() {
                let status=[]
                this.testScriptNames.forEach(testId => {
                    const eventResult = this.$store.state.testRunner.clientTestResult[testId]
                    if (!eventResult) {
                        status[testId] = 'not-run'
                    } else {
                        status[testId] = this.hasSuccessfulEvent(testId) ? 'pass' : 'fail'
                    }
                })
                this.time  = []
                return status
            },
            async testScriptNamesUpdated() {
                if (this.isClient) {
                    return this.$store.state.testRunner.testScriptNames.forEach(name => {
                        this.doEval(name)
                    })
                }
            },
            setEvalCount() {
                this.$store.commit('setEventEvalCount', this.evalCount)
            },
        },
        computed: {
            // status (and time) array is computed to drive display
            status() {
                if (this.isClient)
                    return this.importStatusClientTests()
                else
                    return this.importStatusForServerTests()
            },
            clientBaseAddress() { // for client tests
                return `${this.$store.state.base.proxyBase}/${this.sessionId}__${this.channelId}`
            },
            isClient() {
                return this.$store.state.testRunner.isClientTest
            },
            selected() {
                return this.$store.state.testRunner.currentTest
            },
            testScriptNames() {
                const scripts = this.$store.state.testRunner.testScriptNames
                if (!scripts)
                    return null
                const names = scripts.sort()
                return names
            },
            testReportNames() {  // just the ones with reports available
                const reports = this.$store.state.testRunner.testReports
                if (!reports)
                    return null
                return Object.keys(reports).sort()
            },
            scriptNames: {
                get() {
                    return this.$store.state.testRunner.testScriptNames
                }
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
            this.load()
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
            },
           //'$store.state.testRunner.testScriptNames' : 'testScriptNamesUpdated',
           //'$store.state.testRunner.testReports': 'updateReportStatuses',
           //'$store.state.testRunner.clientTestResult':'evalStatus'
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
    .running {
        background-color: lightgreen;
    }
    .fail {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
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
    .button-selected {
        border: 1px solid black;
        background-color: lightgray;
        cursor: pointer;
    }
    .button-not-selected {
        border: 1px solid black;
        cursor: pointer;
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
    .configurationError {
        color: red;
    }
</style>
