import Vue from 'vue'
import Vuex from 'vuex'
import {ENGINE, LOG} from '../common/http-common'

Vue.use(Vuex)

export const testRunnerStore = {
    state() {
        return {
            clientTestCollectionNames: [],
            serverTestCollectionNames: [],
            currentTestCollectionName: null,
            testScriptNames: [],
            testReportNames: [],

            currentTest: null,  // testId
            currentEvent: null,  // eventId
            currentAssertIndex: null,
            isClientTest: false,  // applies to entire testCollection
            requiredChannel: null,   // applies to entire testCollection
            collectionDescription: null,
            waitingOnClient: null, // testId waiting on or null

            testScripts: [], // testId => TestScript
            testReports: [], // testId => TestReport

            // client eval control
            lastMarker: null,    // evaluate events since OR
            eventEvalCount: 0,   // number of most recent events to evaluate

            clientTestResult: [], // { testId: { eventId: TestReport } }
            currentChannelBaseAddr: 'http://localhost:8081/asbestos/',
            testAssertions: null,
            testCollectionsLoaded: false,
            hapiIsAlive: false,
        }
    },
    mutations: {
        setHapiIsAlive(state, value) {
            state.hapiIsAlive = value
        },
        setRequiredChannel(state, channel) {
            state.requiredChannel = channel
        },
        resetTestCollectionsLoaded(state) {
            state.testCollectionsLoaded = false
        },
        testCollectionsLoaded(state) {
            state.testCollectionsLoaded = true
        },
        setTestAssertions(state, assertions) {
            state.testAssertions = assertions
        },
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
            state.isClientTest = isClient
        },
        setCollectionDescription(state, collectionDescription) {
            state.collectionDescription = collectionDescription
        },
        setTestScriptNames(state, names) {
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
        setTestReport(state, data) {
            state.testReports[data.testName] = data.testReport
        },
        clearTestScripts(state) {
            state.testScripts = []
        },
        addTestScript(state, scriptObject) {
            // scriptObject is  { name: testId, script: TestScript }
            Vue.set(state.testScripts,scriptObject.name, scriptObject.script)
            //state.testScripts.splice(scriptObject.name, 1, scriptObject.script)
            //state.testScripts[scriptObject.name] = scriptObject.script
        },
        setTestCollectionName(state, name) {
            state.currentTestCollectionName = name
        },
        setClientTestCollectionNames(state, names) {
            state.clientTestCollectionNames = names
        },
        setServerTestCollectionNames(state, names) {
            state.serverTestCollectionNames = names
        },
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
        loadTestAssertions({commit}) {
            const url = `assertions`
            ENGINE.get(url)
                .then(response => {
                    commit('setTestAssertions', response.data)
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                })
        },
        runEval({commit, state, rootState}, testId) {
            const eventEval = state.eventEvalCount === 0 ? "marker" : state.eventEvalCount
            const url = `clienteval/${rootState.base.session}__${rootState.base.channelId}/${eventEval}/${state.currentTestCollectionName}/${testId}`
            ENGINE.get(url)
                .then(response => {
                    const results = response.data

                    commit('setClientTestResult', { testId: testId, result: results[testId]} )
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                })
        },
        loadLastMarker({commit, rootState}) {
            const uri = `marker/${rootState.base.session}/${rootState.base.channelId}`
            LOG.get(uri)
                .then(response => {
                    const value = response.data === '' ? 'None' : response.data
                    commit('setLastMarker', value)
                })
                .catch(function (error) {
                    commit('setError', uri + ': ' + error)
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
                    commit('setError', url + ': ' + error)
                })
        },
        loadTestScript({commit}, payload ) {
            const testCollection = payload.testCollection
            const testId = payload.testId
            const url = `collection/${testCollection}/${testId}`
                return ENGINE.get(url)
                    .then(response => {
                        commit('addTestScript', {name: testId, script: response.data})
                    })
                    .catch(function (error) {
                        commit('setError', url + ': ' + error)
                    })
        },
        loadTestCollectionNames({commit}) {
            const url = `collections`
            ENGINE.get(url)
                .then(response => {
                    commit('testCollectionsLoaded')
                    let clientTestNames = []
                    let serverTestNames = []
                    response.data.forEach(collection => {
                        if (collection.server)
                            serverTestNames.push(collection.name)
                        else
                            clientTestNames.push(collection.name)
                    })
                    commit('setClientTestCollectionNames', clientTestNames.sort())
                    commit('setServerTestCollectionNames', serverTestNames.sort())
                })
                .catch(function (error) {
                    this.$store.commit('setError', url + ': ' +  error)
                })
        },
        async loadTestScriptNames({commit, state}) {
            const url = `collection/${state.currentTestCollectionName}`
            try {
                const response = await ENGINE.get(url)
                const theResponse = response.data
                commit('setTestScriptNames', theResponse.testNames)
                const isClient = !theResponse.isServerTest
                commit('setRequiredChannel', theResponse.requiredChannel)
                const description = theResponse.description
                commit('setCollectionDescription', description)
                commit('setIsClientTest', isClient)
                commit('clearTestScripts')
            } catch (error) {
                commit('setError', url + ': ' + error)
            }
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
                        reports[reportName] = response.data[reportName]
                    }
                    commit('setTestReports', reports)
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                })
        },
        hapiHeartbeat({commit}) {
            const url = `hapiheartbeat`
            ENGINE.get(url)
                .then(() => {
                    commit('setHapiIsAlive', true)
                })
                .catch (() => {
                    commit('setHapiIsAlive', false)
                })
        }
    }
}
