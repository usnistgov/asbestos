import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

// TODO add About page and credit <div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/"             title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/"             title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>

export const baseStore = {
    state() {
        return {
            session: 'default',
            // environment: 'default',
            channel: null,  // private communication between ChannelNav and ChannelView
            testSession: null,
            channelId: null,
            testCollectionNames: [],
            currentTestCollectionName: null,
            testScripts: [], // testId => TestScript
            testReports: [], // testId => TestReport

            // private to the log viewer
            eventSummaries: null,
            currentEventIndex: 0,


            sessions: [
                'default', 'ts1'
            ],
            environments: [
                'default', 'e1'
            ],
            channelTypes: [
                'passthrough',
                'mhd'
            ],
            tests: [],
            testIds: [],

            // these two must be in same order
            //
            // fullChannelId can exist without channel - ChannelView.fetch() will notice this
            // and fetch channel from server
            fullChannelIds: [],  // testSession__channelId
        }
    },
    mutations: {
        setSession(state, theSession) {
            state.session = theSession
        },
        // setEnvironment(state, theEnvironment) {
        //     state.environment = theEnvironment
        // },
        clearTestReports(state) {
            state.testReports.length = 0
        },
        addTestReport(state, reportObject) {
            // reportObject is { name: testId, report: TestReport }
            state.testReports[reportObject.name] = reportObject.report
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
        setCurrentEventIndex(state, index) {
            state.currentEventIndex = index
        },
        updateCurrentEventIndex(state, value) {
            state.currentEventIndex += value
        },
        setEventSummaries(state, summaries) {
            state.eventSummaries = summaries
        },
        setChannelId(state, channelId) {
            state.channelId = channelId
        },
        setChannel(state, theChannel) {
            state.channel = theChannel
            if (theChannel === null)
                return
            const fullId = `${theChannel.testSession}__${theChannel.channelId}`
            let channelIndex = state.fullChannelIds.findIndex( function(channelId) {
                return channelId === fullId
            })
            if (channelIndex === -1)
                state.fullChannelIds.push(fullId)
        },
        installChannel(state, newChannel) {  // adds to end
            const thisChannelId = newChannel.testSession + '__' + newChannel.channelId
            let channelIndex = state.fullChannelIds.findIndex( function(channelId) {
                return channelId === thisChannelId
            })
            if (channelIndex === -1) {
                state.fullChannelIds.push(thisChannelId)
                console.log(`install new channel - id=${newChannel.channelId}`)
                state.channel = newChannel
            } else {
                console.log(`install replacement channel - id=${newChannel.channelId}`)
                state.channel = newChannel
            }
        },
        deleteChannel(state, theFullChannelId) {
            const channelIndex = state.fullChannelIds.findIndex( function(channelId) {
                return channelId === theFullChannelId
            })
            if (channelIndex === undefined)
                return
            state.fullChannelIds.splice(channelIndex, 1)
        },
        installChannelIds(state, theFullChannelIds) {
            state.fullChannelIds = theFullChannelIds
        },


        clearTests(state) {
            state.testIds.length = 0
            state.tests.length = 0
        },
        installTestIds(state, testIds) {
            for (let i in testIds) {
                state.testIds.push(testIds[i])
            }
        },
        installTest(state, test) {
            state.tests.push(test)
        },
        installTestVariable(state, variableDesc) {
            // variableDesc is { testId: id, part: variable }
            const testId = variableDesc.testId
            const variable = variableDesc.part
            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testId} in installTestVariable` }
            state.tests[testIndex].variables.push(variable)
        },
        updateTestVariable(state, variable) {
            const testId = variable.testId
            const variableId = variable.id

            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testId} in updateTestVariable` }

            const variableIndex = state.tests[testIndex].variables.findIndex( function (vari) {
                return vari.id === variableId
            })
            if (variableIndex === -1) { throw `Cannot find variable id ${variableId} in test ${testId} in updateTestVariable` }
            state.tests[testIndex].variables[variableIndex] = variable
        },
        installTestFixture(state, testDesc) {
            // testDesc is { testId: id, part: variable }
            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testDesc.testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testDesc.id} in installTestVariable` }
            state.tests[testIndex].fixtures.push(testDesc.part)
        },
        installTestSetup(state, testDesc) {
            // testDesc is { testId: id, part: variable }
            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testDesc.testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testDesc.id} in installTestVariable` }
            state.tests[testIndex].setups.push(testDesc.part)
        },
        installTestTest(state, testDesc) {
            // testDesc is { testId: id, part: variable }
            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testDesc.testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testDesc.id} in installTestVariable` }
            state.tests[testIndex].tests.push(testDesc.part)
        },
        installTestTeardown(state, testDesc) {
            // testDesc is { testId: id, part: variable }
            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testDesc.testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testDesc.id} in installTestVariable` }
            state.tests[testIndex].teardowns.push(testDesc.part)
        }
    },
    getters: {

    }
}
