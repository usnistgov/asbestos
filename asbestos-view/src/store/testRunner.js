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
            testScripts: [], // testId => TestScript
            testReports: [], // testId => TestReport
        }
    },
    mutations: {
        setTestScriptNames(state, names) {
            state.testScriptNames = names.sort()
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
        loadTestScriptNames({commit, state}) {
            if (state.currentTestCollectionName === null)
                console.error(`loadTestScriptNames: state.currentTestCollectionName is null`)
            const that = this
            ENGINE.get(`collection/${state.currentTestCollectionName}`)
                .then(response => {
                    let theResponse = response.data
                    commit('setTestScriptNames', theResponse)
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
