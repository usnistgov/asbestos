import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex)

export const testScriptDebuggerStore = {
    state() {
        return {
            showDebugButton: {},
            breakpointMap: new Map(), /* key = String.format("%d.%d", testCollectionIndex , testScriptIndex). Value (Set) = String.format("%d.%d", testIndex, actionIndex) [0..*] */
            testScriptDebuggerWebSocket: null,
            resumeBreakpointIndex: '',
        }
    },
    mutations: {
        addBreakpoint(state, obj) {
            var breakpointSet = null

            if (! state.breakpointMap.has(obj.testScriptIndex)) {
               state.breakpointMap.set(obj.testScriptIndex, new Set())
            }

            breakpointSet = state.breakpointMap.get(obj.testScriptIndex)

            breakpointSet.add(obj.breakpointIndex)

            if (obj.testScriptIndex in state.showDebugButton === false) {
                Vue.set(state.showDebugButton, obj.testScriptIndex, true) // Add property using Vue.set to nudge reactivity
                console.log(obj.testScriptIndex + "added" + obj.breakpointIndex)
            } else {
                if (state.showDebugButton[obj.testScriptIndex] === false) {
                    Vue.set(state.showDebugButton, obj.testScriptIndex, true)
                }
            }
        },
        removeBreakpoint(state, obj) {
            if (state.breakpointMap.has(obj.testScriptIndex)) {
                var breakpointSet = state.breakpointMap.get(obj.testScriptIndex)
                if (breakpointSet.has(obj.breakpointIndex)) {
                    breakpointSet.delete(obj.breakpointIndex)

                    if (breakpointSet.size == 0) {
                        if (obj.testScriptIndex in state.showDebugButton === true) {
                            Vue.set(state.showDebugButton, obj.testScriptIndex, false) // Add property using Vue.set to nudge reactivity
                            // console.log(obj.testScriptIndex + "removed" + obj.breakpointIndex)
                        }
                    }
                }
            }
        },
        setResumeBreakpoint(state, val) {
           state.resumeBreakpoint = val
        }
    },
    getters: {
        hasBreakpoint: (state) => (obj) => {
            if (state.breakpointMap.has(obj.testScriptIndex)) {
                const breakpointSet = state.breakpointMap.get(obj.testScriptIndex)
                return breakpointSet.has(obj.breakpointIndex)
            }
            return false
        },
        getDebugTitle: (state, getters) => (obj) => {
            if (getters.hasBreakpoint(obj)) {
                return 'Remove breakpoint'
            } else {
                return 'Set breakpoint'
            }
        },

    },
    actions: {
        addBreakpoint({commit}, value) {
            if (value != null && value != undefined) {
                if (value.testScriptIndex != null && value.testScriptIndex != undefined) {
                    if (value.breakpointIndex != null && value.breakpointIndex != undefined) {
                        commit('addBreakpoint', value)
                        return;
                    }
                }
            }
            alert('Requested breakpoint could not be added.')
        },
        removeBreakpoint({commit}, value) {
            commit('removeBreakpoint', value)
        },
        async debugTestScript({commit, rootState, state}, testId) {
            console.log('pass 1' + testId)
            commit('setCurrentTest', testId)
            console.log('pass 2')

            // Technically it is possible to run many debugger web sockets but the test display only allows "opening" one active Test bar at a time since the previous test bar is automatically closed.

            if (state.testScriptDebuggerWebSocket === null) {
                state.testScriptDebuggerWebSocket = new WebSocket('wss://fhirtoolkit.test:9743/asbestos/testScriptDebugger') // TODO: Replace https off the HTTPS TOOLKIT BASE and append the Endpoint
                state.testScriptDebuggerWebSocket.onopen = event => {
                    console.log('In socket onOpen')
                    let testCollectionName = this.state.testRunner.currentTestCollectionName
                    let uri = `debug-testscript/${rootState.base.session}__${rootState.base.channelId}/${testCollectionName}/${testId}?_format=${this.state.testRunner.useJson ? 'json' : 'xml'};_gzip=${this.state.testRunner.useGzip}`
                    let indexOfTestId = this.state.testRunner.testScriptNames.indexOf(testId)
                    if (indexOfTestId > -1) {
                        const testCollectionIndex = this.state.testRunner.serverTestCollectionNames.indexOf(testCollectionName)
                        const mapKey = testCollectionIndex + '.' + indexOfTestId
                        const breakpointSet = state.breakpointMap.get(mapKey) // Follow proper key format
                        let breakpointArrayString = JSON.stringify([...breakpointSet])
                        let sendData = `{"uri":"${uri}","breakpointList":${breakpointArrayString}}`
                        console.log('Sending: ' + sendData)
                        state.testScriptDebuggerWebSocket.send(sendData)
                    } else {
                        console.log(indexOfTestId + ": index not found for testId:  " + testId)
                    }
                }
                state.testScriptDebuggerWebSocket.onclose = function(event) {
                    if (event != null && event != undefined) {
                        console.log('onclose data: ' + event.data)
                    }
                    console.log('In socket onClose. Setting socket to null...')
                    state.testScriptDebuggerWebSocket = null
                    console.log('done.')
                }
                state.testScriptDebuggerWebSocket.onmessage = function(event) {
                    console.log('onMessage: ' + event.data)
                    let returnData = JSON.parse(event.data)
                    if (returnData.messageType === 'final-report') {
                        commit('setCombinedTestReports', returnData.testReport)
                        state.testScriptDebuggerWebSocket.close()
                    } else if (returnData.messageType === 'breakpoint-hit') {
                       console.log('breakpoint hit: ' + returnData.breakpointIndex)
                       commit('setResumeBreakpoint', returnData.breakpointIndex)
                       commit('setCombinedTestReports', returnData.testReport)
                    }
                }
                state.testScriptDebuggerWebSocket.onerror = function(event) {
                    if (event != null && event != undefined) {
                        alert('Error: ' + event.data)
                    }
                }
            } else if (state.resumeBreakpointIndex != '') {
                console.log('Resuming from ' + state.resumeBreakpointIndex)
                state.testScriptDebuggerWebSocket.send('{"resumeBreakpoint":"true"}')
            }

           // const promise = ENGINE.post(url)
            // promise.then(result => {
            //     const reports = result.data
            //     commit('setCombinedTestReports', reports)
            // })
            // return promise
        },
    }
}
