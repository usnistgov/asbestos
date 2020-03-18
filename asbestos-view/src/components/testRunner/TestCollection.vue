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

            <button v-bind:class="{'button-selected': json, 'button-not-selected': !json}" @click="doJson()">JSON</button>
            <button v-bind:class="{'button-selected': !json, 'button-not-selected': json}" @click="doXml()">XML</button>
            <div class="divider"></div>
            <div class="divider"></div>
            <button class="runallbutton" @click="doRunAll()">Run All</button>
        </div>

        <div>
            <div v-for="(name, i) in scriptNames"
                 :key="name + i">
                <div >
                    <div @click="openTest(name)">
                        <div v-bind:class="{
                                pass: status[name] === 'pass' && colorful,
                                'pass-plain': status[name] === 'pass' && !colorful,
                                fail: status[name] === 'fail' && colorful,
                                'fail-plain': status[name] === 'fail' && !colorful,
                                error: status[name] === 'error',
                                'not-run': !status[name],
                            }" class="align-left">

                            <script-status v-if="!statusRight" :status-right="statusRight" :name="name"> </script-status>

                            {{ clean(name) }}
                            <span v-if="!$store.state.testRunner.isClientTest"> --  {{ time[name] }}</span>

                            <span v-if="isClient">
                                <img src="../../assets/validate-search.png"  @click.stop="doEval(name)">
                            </span>
                            <span v-else>
                                <img src="../../assets/press-play-button.png"  @click.stop="doRun(name)">
                            </span>

                            <script-status v-if="statusRight" :status-right="statusRight" :name="name"> </script-status>

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
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import ScriptStatus from "./ScriptStatus";
    export default {

        data() {
            return {
                time: [],   // built as a side-effect of status (computed)
                evalCount: 5,
                channelObj: null,  // channel object
                running: false,
            }
        },
        methods: {
            doJson() {
                this.$store.commit('setUseJson', true)
            },
            doXml() {
                this.$store.commit('setUseJson', false)
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
            async doRun(testName) {  // server tests
                if (!testName)
                    return
                this.running = true
                await this.$store.dispatch('runTest', testName)
                this.running = false
            },
            async doRunAll()  {
                this.running = true
                for (const name of this.scriptNames) {
                    if (this.isClient)
                        await this.doEval(name)
                    else
                        await this.$store.dispatch('runTest', name)
                }
                this.running = false
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
                const promises = []
                promises.push(this.$store.dispatch('loadTestScripts', this.$store.state.testRunner.testScriptNames))
                promises.push(this.$store.dispatch('loadTestReports', this.$store.state.testRunner.currentTestCollectionName))
                await Promise.all(promises)
            },
            testReport(testName) {
                if (!testName)
                    return null
                return this.$store.state.testRunner.testReports[testName]
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
            status() {
                return this.$store.getters.testStatus
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
            json: {
                get() {
                    return this.$store.state.testRunner.useJson
                }
            },
            gzip: {
                set(use) {
                    this.$store.commit('setUseGzip', use)
                },
                get() {
                    return this.$store.state.testRunner.useGzip
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
            'testCollection': 'load',
            'channelId': function(newVal) {
                if (this.channel !== newVal)
                    this.channel = newVal
            },
        },
        mixins: [ errorHandlerMixin, colorizeTestReports ],
        name: "TestCollection",
        props: [
            'sessionId', 'channelId', 'testCollection',
        ],
        components: {
            ScriptStatus
        }
    }
</script>

<style scoped>
    .banner-color {
        background-color: lightgray;
        text-align: left;
    }
    .running {
        background-color: lightgreen;
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
    .align-right {
        text-align: right;
    }
    .align-left {
        text-align: left;
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
<style>
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
        border-radius: 25px;
    }
    .condition-fail {
        background-color: gold;
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
</style>
