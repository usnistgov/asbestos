import Vue from 'vue'
import Vuex from 'vuex'
import {ENGINE, LOG} from '../common/http-common'

Vue.use(Vuex)

export const testRunnerStore = {
    state() {
        return {
            testCollectionNames: [],
            currentTestCollectionName: null,
            testScriptNames: [],
            testReportNames: [],

            currentTest: null,  // testId
            currentEvent: null,  // eventId
            currentAssertIndex: null,
            isClientTest: false,  // applies to entire testCollection
            waitingOnClient: null, // testId waiting on or null

            testScripts: [], // testId => TestScript
            testReports: [], // testId => TestReport

            // client eval control
            lastMarker: null,    // evaluate events since OR
            eventEvalCount: 0,   // number of most recent events to evaluate

            clientTestResult: [], // { testId: { eventId: TestReport } }
            currentChannelBaseAddr: 'http://locahost:8081/asbestos/'
        }
    },
    mutations: {
        setEventEvalCount(state, count) {
            state.eventEvalCount = count
        },
        setLastMarker(state, marker) {
            state.lastMarker = marker
        },
        setWaitingOnClient(state, testId) {
            state.waitingOnClient = testId
        },
        setIsClientTest(state, isClient) {
            //console.log(`client is ${isClient}`)
            state.isClientTest = isClient
        },
        setTestScriptNames(state, names) {
            console.log(`testScriptNames is ${names}`)
            state.testScriptNames = names.sort()
        },
        setCurrentTest(state, currentTestId) {
            state.currentTest = currentTestId
        },
        setCurrentEvent(state, currentEventId) {
            state.currentEvent = currentEventId
        },
        setCurrentAssertIndex(state, index) {
            state.currentAssertIndex = index
        },
        clearTestReports(state) {
            state.testReports = []
        },
        setTestReports(state, reports) {
            state.testReports = reports
        },
        clearTestScripts(state) {
            state.testScripts = []
        },
        addTestScript(state, scriptObject) {
            // scriptObject is  { name: testId, script: TestScript }
            console.log(`setting ${scriptObject.name} to ${scriptObject.script}`)
            Vue.set(state.testScripts,scriptObject.name, scriptObject.script)
            //state.testScripts.splice(scriptObject.name, 1, scriptObject.script)
            //state.testScripts[scriptObject.name] = scriptObject.script
        },
        setTestCollectionName(state, name) {
            state.currentTestCollectionName = name
        },
        setTestCollectionNames(state, names) {
            state.testCollectionNames = names
        },
        // setClientTestResult(state, payload) {
        //     // payload is { evalId: xxx, events: eventId => TestReports }
        //     console.log(`installing evalId is ${payload.evalId}`)
        //     console.log(`installing eventIds are ${Object.getOwnPropertyNames(payload.events)}`)
        //     // each value is eventId => TestReport
        //     state.clientTestResult[payload.evalId] = JSON.parse(JSON.stringify(payload.events))
        // },
        setClientTestResult(state, result) {     // { testId: testId, result: result }
            //state.clientTestResult.splice(result.testId, 1, result.result)
            state.clientTestResult[result.testId] = result.result

            // force vue reaction
            state.clientTestResult.push('foo')
            state.clientTestResult.splice(-1, 1)
        }
    },
    getters: {
        testReportNames(state) {
            return Object.keys(state.testReports).sort()
        }
    },
    actions: {
        runEval({commit, state, rootState}, testId) {
            const eventEval = state.eventEvalCount === 0 ? "marker" : state.eventEvalCount
            const url = `clienteval/${rootState.base.session}__${rootState.base.channelId}/${eventEval}/${state.currentTestCollectionName}/${testId}`
            ENGINE.get(url)
                .then(response => {
                    const results = response.data
                    //console.log(`runEval: results testid = ${Object.getOwnPropertyNames(results)}`)

                    //console.log(`called server - evalId is ${testId}`)
                    //console.log(`events for ${testId} are ${Object.getOwnPropertyNames(results[testId])}`)
//                        commit('setClientTestResult', results)
                    commit('setClientTestResult', { testId: testId, result: results[testId]} )
                })
                .catch(function (error) {
                    this.$store.commit('setError', error)
                    console.error(`${error} - runEval - URL was ${url}`)
                })
        },
        loadLastMarker({commit, rootState}) {
            const uri = `marker/${rootState.base.session}/${rootState.base.channelId}`
            console.log(uri)
            LOG.get(uri)
                .then(response => {
                    const value = response.data === '' ? 'None' : response.data
                    commit('setLastMarker', value)
                })
                .catch(function (error) {
                    this.$store.commit('setError', error)
                    console.error(`${error} - loadLastMarker - URL was ${uri}`)
                })
        },
        setMarker({commit, rootState}) {
            const url  = `marker/${rootState.base.session}/${rootState.base.channelId}`
            LOG.post(url)
                .then(response => {
                    const value = response.data === '' ? 'None' : response.data
                    commit('setLastMarker', value)
                })
                .catch(function (error) {
                    this.$store.commit('setError', error)
                    console.error(`${error} - setMarker - URL was ${url}`)
                })
        },
        loadTestScript({commit, state}, payload ) {
            const testCollection = payload.testCollection
            const testId = payload.testId
            const url = `collection/${testCollection}/${testId}`
            console.info(`load testscript - currently ${testCollection}/${testId} is ${state.testScripts[testId]}`)
//            if (state.testScripts[testId] === undefined) {
                //console.info(`${payload.testId} needs loading`)
                return ENGINE.get(url)
                    .then(response => {
                        //console.info(`loaded test script ${testCollection}/${testId}`)
                        commit('addTestScript', {name: testId, script: response.data})
                        //this.script = response.data
                    })
                    .catch(function (error) {
                        this.$store.commit('setError', error)
                        console.error(`${error} - loadTestScript - URL was ${url}`)
                       // throw error
                    })
  //          }
        },
        loadTestCollectionNames({commit}) {
            const url = `collections`
            ENGINE.get(url)
                .then(response => {
                    let theResponse = response.data
                    //console.info(`TestEnginePanel: loaded ${theResponse.length} test collections`)
                    commit('setTestCollectionNames', theResponse.sort())
                })
                .catch(function (error) {
                    this.$store.commit('setError', error)
                    console.error(`${error} - loadTestCollectionNames - URL was ${url}`)
                })
        },
        loadTestScriptNames({commit, state}) {
            if (state.currentTestCollectionName === null)
                console.error(`loadTestScriptNames: state.currentTestCollectionName is null`)
            const url = `collection/${state.currentTestCollectionName}`
            ENGINE.get(url)
                .then(response => {
                    let theResponse = response.data
                    //console.log(`action: testScriptNames are ${theResponse.testNames}`)
                    commit('setTestScriptNames', theResponse.testNames)
                    const isClient = !theResponse.isServerTest
                    //console.log(`action: isClient - ${isClient}`)
                    commit('setIsClientTest', isClient)
                    commit('clearTestScripts')
                })
                .catch(function (error) {
                    this.$store.commit('setError', error)
                    console.error(`${error} - loadTestScriptNames - URL was ${url}`)
                })
        },
        loadReports({commit, state, rootState}) {
            commit('clearTestReports')
            if (!rootState.base.session || !rootState.base.channelId || !state.currentTestCollectionName)
                return
            const url = `testlog/${rootState.base.session}__${rootState.base.channelId}/${state.currentTestCollectionName}`
            ENGINE.get(url)
                .then(response => {
                    let reports = []
                    for (const reportName of Object.keys(response.data)) {
                        const report = response.data[reportName]
                        reports[reportName] = report
                    }
                    commit('setTestReports', reports)
                })
                .catch(function (error) {
                    this.$store.commit('setError', error)
                    console.error(`${error} - loadReports - URL was ${url}`)
                })
        },
        addTestReport({commit, state}, name, report) {
            let reports = state.testReports
            reports[name] = report
            commit('setTestReports', reports)
        },
    }
}
