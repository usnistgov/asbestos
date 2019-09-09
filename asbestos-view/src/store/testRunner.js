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
            state.testScriptNames = names
        },
        setCurrentTest(state, currentTestId) {
            state.currentTest = currentTestId
        },
        clearTestReports(state) {
            state.testReports.length = 0
        },
        addTestReport(state, reportObject) {
            // reportObject is { name: testId, report: TestReport }
            console.log(`new report for ${reportObject.name}`)
            state.testReports[reportObject.name] = reportObject.report
        },
        addTestScript(state, scriptObject) {
            // scriptObject is  { name: testId, script: TestScript }
            state.testScripts[scriptObject.name] = scriptObject.script
        },
        setTestCollectionName(state, name) {
            console.log(`setTestCollectionName ${name}`)
            state.currentTestCollectionName = name
        },
        setTestCollectionNames(state, names) {
            console.log(`state: testCollectionNames are ${names}`)
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
                    console.log(`...${theResponse}`)
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
            console.info('load reports')
            const url = `testlog/${rootState.base.session}__${rootState.base.channelId}/${state.currentTestCollectionName}`
            console.info(`reports url is ${url}`)
            ENGINE.get(url)
                .then(response => {
                    for (const reportName of Object.keys(response.data)) {
                        const report = response.data[reportName]
                        commit('addTestReport', {name: reportName, report: report})
                    }
                    console.log(`Reports were ${Object.keys(state.testReports)}`)
                })
                .catch(function (error) {
                    console.error(`${error} - ${url}`)
                    dispatch('error', error)
                })
        },
        error(err) {
            console.log(err)
        },

    }
}
