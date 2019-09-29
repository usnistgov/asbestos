<template>
    <div>
        <span class="tool-title">Tests for Collection {{ testCollection }}</span>
        <span class="divider"></span>
        <img id="reload" class="selectable" @click="reload()" src="../assets/reload.png"/>
        <span class="divider"></span>

        <div v-if="$store.state.testRunner.isClientTest" class="right">
            Last Marker: {{ $store.state.testRunner.lastMarker }}
            <span class="selectable" @click="setMarker()">Set</span>
        </div>

        <div class="instruction">
            <span v-if="$store.state.testRunner.isClientTest">Client tests - click spyglass to evaluate client inputs since last Marker</span>
            <span v-else>Server tests - click run button to start test</span>
        </div>

        <div v-for="(name, i) in $store.state.testRunner.testScriptNames"
             :key="name + i">
            <div >
                <div @click="selectTest(name)">
                    <div v-bind:class="{ pass: status[name] === 'pass', fail: status[name] === 'fail', error: status[name] === 'error', 'not-run': status[name] === 'undefined' }">
                        <div v-if="status[name] === 'pass'">
                            <img src="../assets/checked.png" class="right">
                        </div>
                        <div v-else-if="status[name] === 'fail' || status[name] === 'error'">
                            <img src="../assets/error.png" class="right">
                        </div>
                        <div v-else>
                            <img src="../assets/blank-circle.png" class="right">
                        </div>
                        <div v-if="isClient">
                            <img src="../assets/validate-search.png" class="right" @click.stop="doEval(name)">

                        </div>
                        <div v-else>
                            <img src="../assets/press-play-button.png" class="right" @click.stop="doRun(name)">
                        </div>
                        {{ name }}  --  {{ time[name] }}
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
    import {ENGINE} from '../common/http-common'
    import errorHandlerMixin from '../mixins/errorHandlerMixin'

    export default {

        data() {
            return {
                status: [],   // testName => undefined, 'pass', 'fail', 'error'
                time: [],
            }
        },
        methods: {
            evalStatus() { // see GetClientTestEvalResult
                this.status = []
                this.allTestScriptNames().forEach(testId => {
                    const eventResult = this.$store.state.testRunner.clientTestResult[testId]
                    if (!eventResult) {
                        this.status[testId] = 'not-run'
                    } else {
                        for (const eventId in eventResult) {
                            if (eventResult.hasOwnProperty(eventId)) {
                                const testReport = eventResult[eventId]
                                if (testReport.result === 'fail') {
                                    this.status[testId] = 'fail'
                                }
                            }
                        }
                    }
                    this.status[testId] = 'pass'
                })
            },
            doEval(testName) {
                this.$store.dispatch('runEval', testName)
                this.evalStatus()
            },
            doRun(testName) {
                this.$store.commit('setCurrentTest', null)
                const that = this
                ENGINE.post(`testrun/${this.sessionId}__${this.channelId}/${this.testCollection}/${testName}`)
                    .then(response => {
                        this.$store.dispatch('addTestReport', testName, response.data)
                        this.$router.replace(`/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`)
                        this.$store.dispatch('loadTestScriptNames')  // force reload of UI
                    })
                    .catch(error => {
                        that.error(error)
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
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`)
            },
            testReport(testName) {
                return this.$store.state.testRunner.testReports[testName]
            },
            updateReportStatuses() {
                console.log('TestCollection: UpdateReportStatuses')
                let status = []
                let time = []
                this.allTestScriptNames().forEach(testName => {
                    if (this.testReport(testName) === undefined) {
                        status[testName] = 'undefined'
                    } else {
                        status[testName] = this.testReport(testName).result  // 'pass', 'fail', 'error'
                        time[testName] = this.testReport(testName).issued
                    }
                    this.status = status
                    this.time = time
                })

            },
            loadReports() {
                this.$store.dispatch('loadReports')
            },
            allTestScriptNames() {
                return this.$store.state.testRunner.testScriptNames
            },
            isCurrent(testId) {
                return testId === this.$store.state.testRunner.currentTest
            },
            loadLastMarker() {
               if (this.$store.state.testRunner.lastMarker === null)
                    this.$store.dispatch('loadLastMarker')
            },
            setMarker() {
                this.$store.dispatch('setMarker')
            },
        },
        computed: {
            isClient() {
                return this.$store.state.testRunner.isClientTest
            },
            current() {
                return this.$store.state.base.testCollectionDetails.find(item => {
                    return item.name === this.testId
                })
            },
            selected() {
                return this.$store.state.testRunner.currentTest
            },

            testScriptNames() {  // just the ones with reports available
                const reports = this.$store.state.testRunner.testReports
                return Object.keys(reports).sort()
            },
            waitingOnClient: {
                set(name) {
                    this.$store.dispatch('waitOnClient', name)
                },
                get() {
                    return this.$store.state.testRunner.waitingOnClient
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

        },
        created() {
            this.reload()
            this.channel = this.channelId
        },
        mounted() {

        },
        watch: {
            'testCollection': 'reload',  //'loadReports',
            'channelId': function(newVal) {
                if (this.channel !== newVal)
                    this.channel = newVal
                this.loadLastMarker()
            },
            '$store.state.testRunner.testScriptNames' : 'loadReports',
            '$store.state.testRunner.testReports': 'updateReportStatuses',
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
        /*font-size: larger;*/
    }
    .fail {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .error {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .not-run {
        background-color: lightgray;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .right {
        text-align: right;
    }
    .instruction {
        text-align: left;
    }
</style>
