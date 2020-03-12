import Vue from 'vue'
import Vuex from 'vuex'
import {ENGINE, LOG} from '../common/http-common'
import {FHIRTOOLKITBASEURL} from "../common/http-common";

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
            clientTestResult: [], // { testId: { eventId: TestReport } }

            // client eval control
            lastMarker: null,    // evaluate events since OR
            eventEvalCount: 0,   // number of most recent events to evaluate

            currentChannelBaseAddr: `${FHIRTOOLKITBASEURL}/`,
            testAssertions: null,
            testCollectionsLoaded: false,
            hapiIsAlive: false,
            hapiDetails: null,
            xdsIsAlive: false,
            xdsDetails: null,
        }
    },
    mutations: {
        setHapiIsAlive(state, value) {
            state.hapiIsAlive = value
        },
        setHapiDetails(state, value) {
            state.hapiDetails = value
        },
        setXdsIsAlive(state, value) {
            state.xdsIsAlive = value
        },
        setXdsDetails(state, value) {
            state.xdsDetails = value
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
//            console.log(`setTestReports ${Object.keys(reports)}`)
            state.testReports = reports
        },
        setTestReport(state, data) {
//            console.log(`setTestReport ${data.testName}`)
            Vue.set(state.testReports, data.testName, data.testReport)
        },
        clearTestScripts(state) {
            state.testScripts = []
        },
        addTestScript(state, scriptObject) {
            // scriptObject is  { name: testId, script: TestScript }
            Vue.set(state.testScripts,scriptObject.name, scriptObject.script)
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
            Vue.set(state.clientTestResult, result.testId, result.result)
        }
    },
    getters: {
        testReportNames(state) {
            return Object.keys(state.testReports).sort()
        },
        clientTestHasErrors: (state) => (testId) => {
            const report = state.testReports[testId]
            if (!report)
                return false
            return report.test.some(test => {
                return test.action.some(action => {
                    return action.assert.result !== 'pass'
                })
            })
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
        async runTest({commit}, parms) {
            const testName = parms.testName
            const sessionId = parms.sessionId
            const channelId = parms.channelId
            const testCollection = parms.testCollection
            const useJson = parms.useJson
            const gzip = parms.gzip
            if (!testName)
                return
            commit('setCurrentTest', null)
            const url = `testrun/${sessionId}__${channelId}/${testCollection}/${testName}?_format=${useJson ? 'json' : 'xml'};_gzip=${gzip}`
            try {
                const response = await ENGINE.post(url)
                commit('setTestReport', { testName: testName, testReport: response.data })
            } catch (error) {
                this.error(error + ` - ${url}`)
            }
        },
        runEval({commit, state, rootState}, testId) {
            const eventEval = state.eventEvalCount === 0 ? "marker" : state.eventEvalCount
            const url = `clienteval/${rootState.base.session}__${rootState.base.channelId}/${eventEval}/${state.currentTestCollectionName}/${testId}`
            ENGINE.get(url)
                .then(response => {
                    const results = response.data
                    // const resultMap = results[testId]
                    // console.log(`Test ${testId}: ${resultMap}`)
                    // const events = Object.keys(resultMap)
                    // for (const eventId of events) {
                    //     const report = resultMap[eventId]
                    //     console.log(`${eventId}==>${report.result}`)
                    // }

                    commit('setClientTestResult', { testId: testId, result: results[testId]} )
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                })
        },
        async runSingleEventEval({commit, rootState}, parms) {
            const testId = parms.testId
            const eventId = parms.eventId
            const testCollectionName = parms.testCollectionName
            const url = `clienteventeval/${rootState.base.session}__${rootState.base.channelId}/${testCollectionName}/${testId}/${eventId}`
            try {
                const response = await ENGINE.get(url)
                const results = response.data
                commit('setClientTestResult', { testId: testId, result: results[testId] } )
                commit('setTestReport', { testName: testId, testReport: results[testId][eventId] } )
            } catch (error) {
                commit('setError', url + ': ' + error)
            }
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
                        if (!collection.hidden) {
                            if (collection.server)
                                serverTestNames.push(collection.name)
                            else
                                clientTestNames.push(collection.name)
                        }
                    })
                    commit('setClientTestCollectionNames', clientTestNames.sort())
                    commit('setServerTestCollectionNames', serverTestNames.sort())
                })
                .catch(function (error) {
                    this.$store.commit('setError', url + ': ' +  error)
                })
        },
        async loadTestScriptNames({commit, state}) {
//            console.log(`loadTestScriptNames`)
            const url = `collection/${state.currentTestCollectionName}`
            try {
                commit('clearTestScripts')
                const response = await ENGINE.get(url)
                const theResponse = response.data
                commit('setTestScriptNames', theResponse.testNames)
                const isClient = !theResponse.isServerTest
                commit('setRequiredChannel', theResponse.requiredChannel)
                const description = theResponse.description
                commit('setCollectionDescription', description)
                commit('setIsClientTest', isClient)
            } catch (error) {
                commit('setError', url + ': ' + error)
            }
        },
        loadReports({commit, rootState}, testCollectionName) {
            //commit('clearTestReports')
            if (!rootState.base.session || !rootState.base.channelId || !testCollectionName)
                return
            const url = `testlog/${rootState.base.session}__${rootState.base.channelId}/${testCollectionName}`
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
                .then(response => {
                    commit('setHapiIsAlive', response.data.responding)
                    commit('setHapiDetails', response.data.addr)
                })
                .catch (() => {
                    commit('setHapiIsAlive', false)
                })
        },
        xdsHeartbeat({commit}) {
            const url = `xdsheartbeat`
            ENGINE.get(url)
                .then(response => {
                    commit('setXdsIsAlive', response.data.responding)
                    commit('setXdsDetails', response.data.addr)
                })
                .catch (() => {
                    commit('setXdsIsAlive', false)
                })
        }

    }

}
