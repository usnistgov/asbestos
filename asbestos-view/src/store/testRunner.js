import Vue from 'vue'
import Vuex from 'vuex'
import {ENGINE} from '../common/http-common'

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
        }
    },
    mutations: {
        setWaitingOnClient(state, value) {
            state.waitingOnClient = value   // true or false
        },
        setTestScriptNames(state, names, isServerTest) {
            state.testScriptNames = names.sort()
            state.isClientTest = !isServerTest
        },
        setCurrentTest(state, currentTestId) {
            state.currentTest = currentTestId
        },
        clearTestReports(state) {
            state.testReports.length = 0
        },
        setTestReports(state, reports) {
            state.testReports = reports
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
        waitOnClient({commit}, testCollectionId, testId) {
            ENGINE.post(``)
        },
        loadTestCollectionNames({commit}) {
            const that = this
            ENGINE.get(`collections`)
                .then(response => {
                    let theResponse = response.data
                    console.info(`TestEnginePanel: loaded ${theResponse.length} test collections`)
                    commit('setTestCollectionNames', theResponse.sort())
                })
                .catch(function (error) {
                    that.error(error)
                })
        },
        loadTestScriptNames({commit, state}) {
            if (state.currentTestCollectionName === null)
                console.error(`loadTestScriptNames: state.currentTestCollectionName is null`)
            const that = this
            ENGINE.get(`collection/${state.currentTestCollectionName}`)
                .then(response => {
                    let theResponse = response.data
                    commit('setTestScriptNames', theResponse.testNames, theResponse.isServerTest)
                })
                .catch(function (error) {
                    that.error(error)
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
        error(err) {
            console.log(err)
        },

    }
}
