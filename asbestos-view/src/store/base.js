import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

// TODO add About page and credit <div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/"             title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/"             title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>

export const baseStore = {
    state() {
        return {
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
            channels: []
        }
    },
    mutations: {
        installChannel(state, newChannel) {  // adds to end
            const thisChannelId = newChannel.testSession + '__' + newChannel.channelId
            let channelIndex = state.fullChannelIds.findIndex( function(channelId) {
                return channelId === thisChannelId
            })
            if (channelIndex === -1) {
                state.fullChannelIds.push(thisChannelId)
                state.channels.push(newChannel)
            } else {
                state.channels[channelIndex] = newChannel
            }
        },
        installChannelIds(state, channelIds) {  // must be pre-sorted
            state.fullChannelIds.length = 0
            state.channels.length = 0
            for (let i in channelIds) {
                const theId = channelIds[i]
                state.fullChannelIds.push(theId)
                state.channels.push(null)
            }
        },
        deleteChannel(state, theFullChannelId) {
            const channelIndex = state.fullChannelIds.findIndex( function(channelId) {
                return channelId === theFullChannelId
            })
            state.fullChannelIds.splice(channelIndex, 1)
            state.channels.splice(channelIndex, 1)
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
