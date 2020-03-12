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


            testReportNames: [],

            currentTest: null,  // testId
            currentEvent: null,  // eventId
            currentAssertIndex: null,
//            waitingOnClient: null, // testId waiting on or null

            testReports: {}, // testId => TestReport

            // client eval control
            lastMarker: null,    // evaluate events since OR
            eventEvalCount: 0,   // number of most recent events to evaluate

            clientTestResult: [], // { testId: { eventId: TestReport } }
//            currentChannelBaseAddr: `${FHIRTOOLKITBASEURL}/`,
            testAssertions: null,
            debug: null,
        }
    },
    mutations: {
        setDebugValue(state, value) {
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
        setTestScripts(state, scripts) {
            state.testScripts = scripts
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
        runSingleEventEval({commit, rootState}, parms) {
            const testId = parms.testId
            const eventId = parms.eventId
            const testCollectionName = parms.testCollectionName
            const url = `clienteventeval/${rootState.base.session}__${rootState.base.channelId}/${testCollectionName}/${testId}/${eventId}`
            ENGINE.get(url)
                .then(response => {
                    const results = response.data
                    commit('setClientTestResult', { testId: testId, result: results[testId] } )
                    commit('setTestReport', { testName: testId, testReport: results[testId][eventId] } )
                })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                })
        },
        // expect currentTestCollectionName to be set
        loadTestScripts({commit, state}, scriptNames) {
            commit('clearTestScripts')
            const promises = []
            scriptNames.forEach(name => {
                const url = `testScript/${state.currentTestCollectionName}/${name}`
                const promise = ENGINE.get(url)
                promises.push(promise)
            })
            const scripts = {}
            return Promise.all(promises)
                .then(results => {
                    results.forEach(result => {
                        const script = result.data
                        scripts[script.name] = script
                    })
                    commit('setTestScripts', scripts)
                })
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
        // loadReports({commit, rootState}, parms) {
        //     const testCollectionId = parms.testCollectionId
        //     // const testId = parms.testId
        //     // commit('clearTestReports')
        //     // if (!rootState.base.session || !rootState.base.channelId || !testCollectionId || !testId)
        //     //     return
        //     // const promises = []
        //     // state.testScriptNames.forEach(testId => {
        //     //     const url = `testlog/${rootState.base.session}__${rootState.base.channelId}/${testCollectionId}/${testId}`
        //     //     promises.push(ENGINE.get(url))
        //     // })
        //     // const reports = {}
        //     // return Promise.all(promises)
        //     //     .then(results => {
        //     //         results.forEach(result => {
        //     //
        //     //         })
        //     //     })
        //
        //
        //     const url = `testlog/${rootState.base.session}__${rootState.base.channelId}/${testCollectionId}`
        //     ENGINE.get(url)
        //         .then(response => {
        //             let reports = []
        //             for (const reportName of Object.keys(response.data)) {
        //                 reports[reportName] = response.data[reportName]
        //             }
        //             commit('setTestReports', reports)
        //         })
        //         .catch(function (error) {
        //             commit('setError', url + ': ' + error)
        //         })
        // },
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
                commit('clearTestScripts')  // clears testScripts
                const response = await ENGINE.get(url)
                const theResponse = response.data
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
