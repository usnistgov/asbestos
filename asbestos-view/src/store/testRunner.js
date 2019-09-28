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

            currentTest: null,  // testId
            isClientTest: false,  // applies to entire testCollection
            waitingOnClient: null, // testId waiting on or null

            testScripts: [], // testId => TestScript
            testReports: [], // testId => TestReport

            lastMarker: null,
        }
    },
    mutations: {
        setLastMarker(state, marker) {
            state.lastMarker = marker
        },
        setWaitingOnClient(state, testId) {
            state.waitingOnClient = testId
        },
        setIsClientTest(state, isClient) {
            console.log(`client is ${isClient}`)
            state.isClientTest = isClient
        },
        setTestScriptNames(state, names) {
            state.testScriptNames = names.sort()
        },
        setCurrentTest(state, currentTestId) {
            state.currentTest = currentTestId
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
            state.testScripts[scriptObject.name] = scriptObject.script
        },
        setTestCollectionName(state, name) {
            state.currentTestCollectionName = name
        },
        setTestCollectionNames(state, names) {
            state.testCollectionNames = names
        },
    },
    getters: {
        testReportNames(state) {
            return Object.keys(state.testReports).sort()
        }
    },
    actions: {
        loadLastMarker({commit, rootState}) {
            LOG.get(`marker/${rootState.base.session}/${rootState.base.channelId}`)
                .then(response => {
                    const value = response.data === '' ? 'None' : response.data
                    commit('setLastMarker', value)
                })
                .catch(function (error) {
                    console.error(error)
                })
        },
        setMarker({commit, rootState}) {
            LOG.post(`marker/${rootState.base.session}/${rootState.base.channelId}`)
                .then(response => {
                    const value = response.data === '' ? 'None' : response.data
                    commit('setLastMarker', value)
                })
                .catch(function (error) {
                    console.error(error)
                })
        },
        waitOnClient({commit, state, rootState}, testId) {
            ENGINE.post(`eval/${rootState.base.session}__${rootState.base.channelId}/${state.currentTestCollectionName}/${testId}`)
                .then(response => {
                    commit('setWaitingOnClient', testId)
                })
                .catch(function (error) {
                    console.error(error)
                })
        },
        loadTestCollectionNames({commit}) {
            ENGINE.get(`collections`)
                .then(response => {
                    let theResponse = response.data
                    console.info(`TestEnginePanel: loaded ${theResponse.length} test collections`)
                    commit('setTestCollectionNames', theResponse.sort())
                })
                .catch(function (error) {
                    console.error(error)
                })
        },
        loadTestScriptNames({commit, state}) {
            if (state.currentTestCollectionName === null)
                console.error(`loadTestScriptNames: state.currentTestCollectionName is null`)
            const url = `collection/${state.currentTestCollectionName}`
            ENGINE.get(url)
                .then(response => {
                    let theResponse = response.data
                    commit('setTestScriptNames', theResponse.testNames)
                    const isClient = !theResponse.isServerTest
                    commit('setIsClientTest', isClient)
                    commit('clearTestScripts')
                })
                .catch(function (error) {
                    console.error(`${error} - ${url} failed`)
                })
        },
        loadReports({dispatch, commit, state, rootState}) {
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
                    console.error(`${error} - ${url}`)
                    dispatch('error', error)
                })
        },
        addTestReport({commit, state}, name, report) {
            let reports = state.testReports
            reports[name] = report
            commit('setTestReports', reports)
        },
    }
}
