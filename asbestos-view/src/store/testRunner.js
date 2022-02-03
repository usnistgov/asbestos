import Vue from 'vue'
import Vuex from 'vuex'
import {ENGINE, LOG} from '../common/http-common'
//import {FHIRTOOLKITBASEURL} from "../common/http-common";

Vue.use(Vuex)

export const testRunnerStore = {
    state() {
        return {
            // testCollections (for listings in control panel)
            // loaded by action loadTestCollectionNames
            clientTestCollectionObjs: [],
            serverTestCollectionObjs: [],
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
            testAssertions: null, // {},
            debug: null,
            useJson: true,
            useGzip: true,
            colorMode: false,
            statusRight: false,
            hapiFhirBase: null,
            autoRoute: false,

            ftkTestDependencies: {},
            nonCurrentTcTestReports: {},

            doReloadTestCollectionObjs: function(state, tcObj, tcObjs) {
                if (tcObj in state) {
                    if (state[tcObj].length > 0) {
                        state[tcObj].splice(0, state[tcObj].length)
                    }
                    for (let r of tcObjs) {
                        state[tcObj].push(r)
                    }
                }
            },

            filterTestCollectionsMhdVersion: function(state, rootState, tcObjs, mhdVersions) {
                if (mhdVersions === undefined) {
                    mhdVersions = rootState.base.channel.mhdVersions
                }
                if (mhdVersions !== null && Array.isArray(mhdVersions)) {
                    // console.log(String(tcObjs.map(e=>e.mhdVersion)))
                    return tcObjs.filter(e => mhdVersions.includes(e.mhdVersion)).map(e => e.name)
                }
                return tcObjs.map(e => e.name)
            },

            sortTestCollection: function(l,r) {
                return l.name.localeCompare(r.name)
            }
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
            /*
            if (assertions !== null && assertions !== undefined) {
                for (let assertionId in assertions) {
                    Vue.set(state.testAssertions, assertionId, assertions[assertionId])
                }
            }
             */
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
            let keys = Object.keys(state.testReports)
            for (const k of keys) {
                Vue.delete(state.testReports, k)
            }
            keys = Object.keys(state.moduleTestReports)
            for (const k of keys) {
                Vue.delete(state.moduleTestReports, k)
            }

        },
        clearTestReport(state, testNameToRemove) {
            if (state.testReports !== null && state.testReports !== undefined && Array.isArray(state.testReports) && state.testReports.length > 0) {
                for (let testName in state.testReports) {
                    const report = state.testReports[testName]
                    if (report && report.resourceType === 'TestReport') {
                        if (testName === testNameToRemove) {
                            state.testReports.splice(testName, 1)
                            for (let moduleName in state.moduleTestReports) {
                               if (moduleName.startsWith(testNameToRemove + '/'))  {
                                   state.moduleTestReports.splice(moduleName, 1)
                               }
                            }
                        }
                    }
                }
            }
        },
        setTestReport(state, report) {
            Vue.set(state.testReports, report.name, report)
        },
        setTestReports(state, reports) {
            state.testReports = reports
        },
        clearTestScripts(state) {
            let keys = Object.keys(state.testScripts)
            for (const k of keys) {
                Vue.delete(state.testScripts, k)
            }
            keys = Object.keys(state.moduleTestReports)
            for (const k of keys) {
                Vue.delete(state.moduleTestReports, k)
            }
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
          //  console.log(`setCombinedTestReports`)
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
        setClientTestCollectionObjs(state, objs) {
            state.doReloadTestCollectionObjs(state, 'clientTestCollectionObjs', objs)
        },
        setServerTestCollectionObjs(state, objs) {
            state.doReloadTestCollectionObjs(state, 'serverTestCollectionObjs', objs)
        },
        setClientTestResult(state, parms) {     // { testId: testId, result: result }
            // console.info('In setClientTestResult')
            const eventReports = parms.reports
            // Partition the modular reports
            // let obj = {}
            Vue.set(state.clientTestResult, parms.testId, eventReports)
            /*
            For client test collections, ClientDetails.vue selectEvent method loads module reports when a particular event is selected by the user.
            For the Inspector tool, the following partitioning is required since components can now have other components.
            */
            for (let eventId in eventReports) {
                // let mainReports = []
               for (let testReport of eventReports[eventId]) {
                   // console.log('processing index #...')
                   if (testReport !== null && testReport !== undefined && 'resourceType' in testReport) {
                       if (testReport.resourceType === 'TestReport') {
                           let testName = testReport.name
                           if (testName.includes("/"))
                               Vue.set(state.moduleTestReports, testName, testReport)
                           // else
                           //     mainReports.push(testReport)
                       } else {
                           console.error('Unexpected resourceType: ' + testReport.resourceType)
                       }
                   } else {
                       console.error('Unexpected resource in eventReports')
                   }
               }
               // obj[eventId] = mainReports
            }
        },
        addTestArtifactDependency(state, paramObj) {
            Vue.set(state.ftkTestDependencies, paramObj.testArtifactId, paramObj.depTaIds)
        },
        addNonCurrentTcTestReport(state, paramObj) {
            Vue.set(state.nonCurrentTcTestReports, paramObj.testArtifactId, paramObj.testReport)
        },
    },
    getters: {
        clientTestCollectionNames: (state,rootState) => (mhdVersions) => {
            return state.filterTestCollectionsMhdVersion(state, rootState, state.clientTestCollectionObjs, mhdVersions)
        },
        serverTestCollectionNames: (state,rootState) => (mhdVersions) => {
            return state.filterTestCollectionsMhdVersion(state, rootState, state.serverTestCollectionObjs, mhdVersions)
        },
        allServerTestCollectionNames: (state) => {
            return state.serverTestCollectionObjs.map(e => e.name)
        },
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
        loadTestAssertions({commit}) {
            const url = `assertions`
            ENGINE.get(url)
                .then(response => {
                    // console.log(JSON.stringify(response.data))
                    console.info(response.data.id + ' has ' + Object.keys(response.data.assertionReferences).length + ' assertions.')
                    commit('setTestAssertions', response.data)
                })
                .catch(function (error) {
                    commit('setError', ENGINE.baseURL  + url + ': ' + error)
                })
        },
        runEval({commit, state, rootState}, testId) {
            const url = `clienteval/${rootState.base.channel.testSession}__${rootState.base.channel.channelName}/${state.eventEvalCount}/${state.currentTestCollectionName}/${testId}`
            ENGINE.get(url)
                .then(response => {
                    const reports = response.data
                    commit('setClientTestResult', { testId: testId, reports: reports[testId]} )
                })
                .catch(function (error) {
                    commit('setError', LOG.baseURL + url + ': ' + error)
                })
        },
        runSingleEventEval({commit, rootState}, parms) {
            const testId = parms.testId
            const eventId = parms.eventId
            const testCollectionName = parms.testCollectionName
            const url = `clienteventeval/${rootState.base.channel.testSession}__${rootState.base.channel.channelName}/${testCollectionName}/${testId}/${eventId}`
            ENGINE.get(url)
                .then(response => {
                    const results = response.data
                    commit('setClientTestResult', { testId: testId, reports: results[testId] } )
                    // setTestReport is not really useful if modular test scripts were used and multiple reports are compressed all into one report file
                    // commit('setTestReport', results[testId][eventId] )
                })
                .catch(function (error) {
                    commit('setError', ENGINE.baseURL + url + ': ' + error)
                })
        },
        loadTestScripts({commit, state}, scriptNames) {
            commit('clearTestScripts')
            console.info(`scriptNames to be loaded = ${scriptNames}`)
            const promises = []
            scriptNames.forEach(name => {
                const url = `testScript/${state.currentTestCollectionName}/${name}`
                // console.log(`loadTestScripts ${url}`)
                const promise = ENGINE.get(url)
                promises.push(promise)
            })
            const scripts = {}
            const moduleScripts = {}
            return Promise.all(promises)
                .then(results => {
                    results.forEach(result => {
                        const scriptData = result.data
                        if (scriptData !== null && scriptData !== undefined) {
                            for (let testName in scriptData) {
                                const script = scriptData[testName]
                                if (testName.includes('/'))
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
        async loadNonCurrentTcTestReports({commit, rootState, state}) {
            //  begin dependencies part
            const ftkTestDepKeys = Object.keys(state.ftkTestDependencies)
            if (ftkTestDepKeys.length) {
                const testCollectionId = state.currentTestCollectionName
                // Only deal with current test collection but load its non-current test collection dependencies
                const currentTestCollectionDependencies = ftkTestDepKeys.filter(e => e.startsWith(testCollectionId))
                if (currentTestCollectionDependencies.length) {
                    // if index of tc separator is true, use testReport/, else use tclogs/
                   for (const testArtifactId of currentTestCollectionDependencies) {
                       // Only cache non-current test collection test reports
                       if (testArtifactId.startsWith(testCollectionId))
                         continue
                      if (testArtifactId.endsWith("/")) {
                          const tcId = testArtifactId.slice(0,-1)
                          const tcLogsUrl = `tclogs/${rootState.base.channel.testSession}__${rootState.base.channel.channelName}/${tcId}`
                          ENGINE.get(tcLogsUrl)
                              .then(result => {
                                  const trArray = result.data
                                  if (Array.isArray(trArray) && trArray.length) {
                                      for (let idx = 0; idx < trArray.length; idx++) {
                                          const trObj = trArray[idx]
                                          try {
                                              if (trObj.TestReport.resourceType === 'TestReport') {
                                                  commit('addNonCurrentTcTestReport', {
                                                      testArtifactId: testArtifactId + trObj.TestReport.name,
                                                      testReport: trObj.TestReport
                                                  })
                                              }
                                          } catch  {
                                             console.log(`tclogs entry index ${idx} in ${tcId} does not contain a TestReport.`)
                                          }
                                      }
                                  }
                              })
                              .catch(function(error) {
                                  commit('setError', `Loading non-current test collection test reports for ${tcId} using tclogs failed: ${error}`)
                              })

                      } else if (testArtifactId.includes("/")) {
                          const parts = testArtifactId.split("/",2)
                          const tcId = parts[0]
                          const testName = parts[1]
                          const url = `testReport/${rootState.base.channel.testSession}__${rootState.base.channel.channelName}/${tcId}/${testName}`
                          ENGINE.get(url)
                              .then(result => {
                                  const trObjs = result.data
                                  const theTr = trObjs[testName]
                                  if (theTr !== undefined || theTr !== null) {
                                      commit('addNonCurrentTcTestReport', {testArtifactId: testArtifactId, testReport: theTr})
                                  } else {
                                      console.log('testArtifact theTr is not usable.')
                                  }
                              })
                              .catch(function(error) {
                                  commit('setError', `Loading non-current test collection test reports for ${testArtifactId} using testReport failed: ${error}`)
                              })
                      }
                   }
                    // create new state.interDependentTestReports
                    // use a new computed property to check both the state.testReports and state.interDependentTestReports
                }
            }
            // end.
        },
        async loadTestReports({commit, rootState, state}, testCollectionId) {
            commit('clearTestReports')
            const promises = []
            state.testScriptNames.forEach(name => {
                const url = `testReport/${rootState.base.channel.testSession}__${rootState.base.channel.channelName}/${testCollectionId}/${name}`
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
        async loadTestReport({commit, rootState}, parms) {
            const testCollectionId = parms.testCollectionId
            const testId = parms.testId
            const url = `testReport/${rootState.base.channel.testSession}__${rootState.base.channel.channelName}/${testCollectionId}/${testId}`
            let report = ""
            const promise = ENGINE.get(url)
            promise.then(result => {
                report = result.data
            })
            await promise
            commit('setCombinedTestReports', report)
            return report
        },
        async loadTestScript({commit}, parms) {
            const testCollectionId = parms.testCollection
            const testId = parms.testId
            const url = `testScript/${testCollectionId}/${testId}`
            let script = ""
            const promise = ENGINE.get(url)
            promise.then(result => {
                script = result.data
            })
                .catch(function(error) {
                    commit('setError', `Loading script from ${url} - ${error}`)
                })
            await promise
            commit('setTestScript', script)
            return script
        },
        runTest({commit, rootState, state}, testId) {
           // console.log(`run ${testId}`)
            //commit('setCurrentTest', testId)
            const url = `testrun/${rootState.base.channel.testSession}__${rootState.base.channel.channelName}/${state.currentTestCollectionName}/${testId}?_format=${state.useJson ? 'json' : 'xml'};_gzip=${state.useGzip}`
            const promise = ENGINE.post(url)
            promise.then(result => {
                const reports = result.data
                commit('setCombinedTestReports', reports)
            })
            return promise
        },
        async loadTestCollectionNames({commit, state}) {
            const url = 'collections'
            try {
                ENGINE.get(url)
                    .then(response => {
                        commit('testCollectionsLoaded')  // startup heartbeat for test engine
                        commit('setClientTestCollectionObjs', response.data.filter(e => !e.hidden && !e.server).sort(state.sortTestCollection))
                        commit('setServerTestCollectionObjs', response.data.filter(e => !e.hidden && e.server).sort(state.sortTestCollection))
                    })
                .catch(function (error) {
                    commit('setError', url + ': ' + error)
                    console.error(`${error} for ${url}`)
                })

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
                if ('testNames' in theResponse)
                    commit('setTestScriptNames', theResponse.testNames)  // sets testScriptNames
                if ('requiredChannel' in theResponse)
                    commit('setRequiredChannel', theResponse.requiredChannel)  // sets requiredChannel
                if ('description' in theResponse) {
                    const description = theResponse.description
                    commit('setCollectionDescription', description)  // sets collectionDescription
                }
                if ('isServerTest' in theResponse) {
                    const isClient =  ! theResponse.isServerTest
                    commit('setIsClientTest', isClient)   // sets isClientTest
                }
                if ('testDependencies' in theResponse) {
                    // NOTE: this simply overwrites existing entries, should be OK to clear the testDependencies data if needed
                    for (let testArtifactId in theResponse.testDependencies) {
                        const val = theResponse.testDependencies[testArtifactId]
                        if (Array.isArray(val)) {
                            commit('addTestArtifactDependency', {testArtifactId: testArtifactId, depTaIds: val})
                        }
                    }
                }

            } catch (error) {
                commit('setError', ENGINE.baseURL + url + ': ' + error)
            }
        },


    }

}
