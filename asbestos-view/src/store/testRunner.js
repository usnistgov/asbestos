import Vue from 'vue'
import Vuex from 'vuex'
import {ENGINE} from '../common/http-common'
//import {FHIRTOOLKITBASEURL} from "../common/http-common";

Vue.use(Vuex)

export const testRunnerStore = {
    state() {
        return {
            // testCollections (for listings in control panel)
            // loaded by action loadTestCollectionNames
            clientTestCollectionNames: [],
            serverTestCollectionNames: [],
            testCollectionsLoaded: false,  // used as a startup heartbeat for test engine

            // Only one testCollection is loaded at any time
            currentTestCollectionName: null,

            testScriptNames: [],
            requiredChannel: null,   // applies to entire testCollection
            isClientTest: false,  // applies to entire testCollection
            collectionDescription: null,

            testScripts: {}, // testId => TestScript
            testReports: {}, // testId => TestReport

            moduleTestScripts: {}, // moduleId => TestScript
            moduleTestReports: {},  // testId/moduleId => TestReport

            testReportNames: [],

            currentTest: null,  // testId
            currentEvent: null,  // eventId
            currentAssertIndex: null,

            eventEvalCount: 0,   // number of most recent events to evaluate

            clientTestResult: {}, // { testId: { eventId: TestReport } }
            testAssertions: null,
            debug: null,
            useJson: true,
            useGzip: true,
            colorMode: false,
            statusRight: false,
            hapiFhirBase: null,
            autoRoute: false,
        }
    },
    mutations: {
        setAutoRoute(state, value) {
            state.autoRoute = value;
        },
        setHapiFhirBase(state, value) {
            state.hapiFhirBase = value;
        },
        setCurrentTestCollection(state, value) {
            state.currentTestCollectionName = value;
        },
        setStatusRight(state, value) {
            state.statusRight = value
        },
        setColorMode(state, value) {
            state.colorMode = value
        },
        setUseJson(state, value) {
            state.useJson = value;
        },
        setUseGzip(state, value) {
            state.useGzip = value
        },
        setDebug(state, value) {
            state.debug = value
        },
        // Test Script listing
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
            state.testReports = {}
            state.moduleTestReports = {}
        },
        setTestReport(state, report) {
            Vue.set(state.testReports, report.name, report)
        },
        setTestReports(state, reports) {
            state.testReports = reports
        },
        clearTestScripts(state) {
            state.testScripts = {}
            state.moduleTestReports = {}
        },
        setTestScript(state, script) {
            Vue.set(state.testScripts, script.name, script)
        },
        setTestScriptModule(state, script) {
            Vue.set(state.moduleTestScripts, script.name, script)
        },
        setTestReportModule(state, report) {
            Vue.set(state.moduleTestReports, report.name, report)
        },
        setTestScripts(state, scripts) {
            state.testScripts = scripts
        },
        setModuleTestScripts(state, scripts) {
            state.moduleTestScripts = scripts
        },
        setModuleTestReports(state, reports) {
            state.moduleTestReports = reports
        },
        setCombinedTestReports(state, reportData) {
            console.log(`setCombinedTestReports`)
            for (let testName in reportData) {
                const report = reportData[testName]
                if (report && report.resourceType === 'TestReport') {
                    if (report.extension) {
                        report.extension.forEach(e => {
                            if (e.url === 'urn:failure') {
                                this.commit('setError', e.valueString)
                            }
                        })
                    }
                    if (testName.includes("/"))
                        Vue.set(state.moduleTestReports, testName, report)
                    else
                        Vue.set(state.testReports, testName, report)
                }
            }
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
        setClientTestResult(state, parms) {     // { testId: testId, result: result }
            Vue.set(state.clientTestResult, parms.testId, parms.reports)
        }
    },
    getters: {
        testReportNames(state) {
            return Object.keys(state.testReports).sort()
        },
        testStatus(state, getters) {
            if (state.isClientTest) {
                let status={}
                state.testScriptNames.forEach(testId => {
                    const eventResult = state.clientTestResult[testId]
                    if (!eventResult) {
                        status[testId] = 'not-run'
                    } else {
                        status[testId] = getters.hasSuccessfulEvent(testId) ? 'pass' : 'fail'
                    }
                })
                return status
            } else {
                let status = {}
                state.testScriptNames.forEach(testId => {
                    const testReport = state.testReports[testId]
                    if (testReport === undefined) {
                        status[testId] = 'not-run'
                    } else {
                        status[testId] = testReport.result  // 'pass', 'fail', 'error'
                    }
                })
                return status
            }
        },
        hasSuccessfulEvent: (state) => (testId) => {
            if (testId === null)
                return false
            const eventResult = state.clientTestResult[testId]
            for (const eventId in eventResult) {
                if (eventResult.hasOwnProperty(eventId)) {
                    let testReport = eventResult[eventId]
                    if (Array.isArray(testReport)) {
                        testReport = testReport[0];
                    }
                    if (testReport.result === 'pass')
                        return  true
                }
            }
            return false
        },
    },
    actions: {
        async loadHapiFhirBase({commit}) {
            if (this.hapiFhirBase !== null)
                return;
            const result = await ENGINE.get('hapiFhirBase');
            commit('setHapiFhirBase', result.data);
        },
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
            const url = `clienteval/${rootState.base.session}__${rootState.base.channelId}/${state.eventEvalCount}/${state.currentTestCollectionName}/${testId}`
            ENGINE.get(url)
                .then(response => {
                    const reports = response.data
                    commit('setClientTestResult', { testId: testId, reports: reports[testId]} )
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                })
        },
        runSingleEventEval({commit, rootState}, parms) {
            const testId = parms.testId
            const eventId = parms.eventId
            const testCollectionName = parms.testCollectionName
            const url = `clienteventeval/${rootState.base.session}__${rootState.base.channelId}/${testCollectionName}/${testId}/${eventId}`
            ENGINE.get(url)
                .then(response => {
                    const results = response.data
                    commit('setClientTestResult', { testId: testId, reports: results[testId] } )
                    commit('setTestReport', results[testId][eventId] )
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                })
        },
        loadTestScripts({commit, state}, scriptNames) {
            commit('clearTestScripts')
            const promises = []
            scriptNames.forEach(name => {
                const url = `testScript/${state.currentTestCollectionName}/${name}`
                const promise = ENGINE.get(url)
                promises.push(promise)
            })
            const scripts = {}
            const moduleScripts = {}
            return Promise.all(promises)
                .then(results => {
                    results.forEach(result => {
                        const scriptData = result.data
                        if (scriptData) {
                            for (let testName in scriptData) {
                                const script = scriptData[testName]
                                if (testName.includes("/"))
                                    moduleScripts[testName] = script
                                else
                                    scripts[testName] = script
                            }
                        }
                    })
                    commit('setTestScripts', scripts)
                    commit('setModuleTestScripts', moduleScripts)
                })
        },
        async loadTestReports({commit, rootState, state}, testCollectionId) {
            commit('clearTestReports')
            const promises = []
            state.testScriptNames.forEach(name => {
                const url = `testReport/${rootState.base.session}__${rootState.base.channelId}/${testCollectionId}/${name}`
                const promise = ENGINE.get(url)
                promises.push(promise)
            })
            const combinedPromises = Promise.all(promises)
                .then(results => {
                    results.forEach(result => {
                        const reportData = result.data
                        if (reportData) {
                            commit('setCombinedTestReports', reportData)
                        }
                    })
                })
                .catch(function(error) {
                    commit('setError', `Loading reports: ${error}`)
                })
            await combinedPromises
        },
        async loadTestScript({commit}, parms) {
            const testCollectionId = parms.testCollection
            const testId = parms.testId
            const url = `testScript/${testCollectionId}/${testId}`
            let script = ""
            const promise = ENGINE.get(url)
            promise.then(result => {
                console.log(`test script loaded`)
                script = result.data
            })
                .catch(function(error) {
                    commit('setError', `Loading script from ${url} - ${error}`)
                })
            await promise
            commit('setTestScript', script)
            return script
        },
        async loadTestReport({commit, rootState}, parms) {
            const testCollectionId = parms.testCollectionId
            const testId = parms.testId
            const url = `testReport/${rootState.base.session}__${rootState.base.channelId}/${testCollectionId}/${testId}`
            let report = ""
            const promise = ENGINE.get(url)
            promise.then(result => {
                    report = result.data
                })
            await promise
            commit('setCombinedTestReports', report)
            return report
        },
        runTest({commit, rootState, state}, testId) {
            console.log(`run ${testId}`)
            //commit('setCurrentTest', testId)
            const url = `testrun/${rootState.base.session}__${rootState.base.channelId}/${state.currentTestCollectionName}/${testId}?_format=${state.useJson ? 'json' : 'xml'};_gzip=${state.gzip}`
            const promise = ENGINE.post(url)
            promise.then(result => {
                const reports = result.data
                commit('setCombinedTestReports', reports)
            })
            return promise
        },
        async loadTestCollectionNames({commit}) {
            const url = `collections`
            try {
                const response = await ENGINE.get(url)
                commit('testCollectionsLoaded')  // startup heartbeat for test engine
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
            } catch (error) {
                this.$store.commit('setError', url + ': ' +  error)
            }
        },
        async loadCurrentTestCollection({commit, state}) {
            const url = `collection/${state.currentTestCollectionName}`
            try {
                //commit('clearTestScripts')  // clears testScripts
                let theResponse = ""
                const promise = ENGINE.get(url)
                    .then(response => {
                        theResponse = response.data
                })
                await promise
                commit('setTestScriptNames', theResponse.testNames)  // sets testScriptNames
                const isClient = !theResponse.isServerTest
                commit('setRequiredChannel', theResponse.requiredChannel)  // sets requiredChannel
                const description = theResponse.description
                commit('setCollectionDescription', description)  // sets collectionDescription
                commit('setIsClientTest', isClient)   // sets isClientTest
            } catch (error) {
                commit('setError', url + ': ' + error)
            }
        },
        // this exists because loadCurrentTestCollection updates currentTestCollectionName
        // which causes TestControlPanel2 to auto-route to test display
        // this was first used for self tests.
        async loadTestCollection({commit}, testCollectionName) {
            const url = `collection/${testCollectionName}`
            try {
                //commit('clearTestScripts')  // clears testScripts
                let theResponse = ""
                const promise = ENGINE.get(url)
                    .then(response => {
                        theResponse = response.data
                    })
                await promise
                commit('setTestScriptNames', theResponse.testNames)  // sets testScriptNames
                const isClient = !theResponse.isServerTest
                commit('setRequiredChannel', theResponse.requiredChannel)  // sets requiredChannel
                const description = theResponse.description
                commit('setCollectionDescription', description)  // sets collectionDescription
                commit('setIsClientTest', isClient)   // sets isClientTest
            } catch (error) {
                commit('setError', url + ': ' + error)
            }
        },


    }

}
