import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex)

export const testScriptDebuggerStore = {
    state() {
        return {
            showDebugButton: {}, /* keyProperty{testScriptIndex:}=value{breakpointIndex:,debugButtonLabel:} */
            /* debugButtonLabel exists inside of showDebugButton because there are multiple testscripts and it is necessary to keep showing the Debug button labels for other test scripts.
            If a single button label variable was used then all of the Debug buttons would be become changed to Resume when a breakpoint is hit */
            breakpointMap: new Map(), /* key = String.format("%d.%d", testCollectionIndex , testScriptIndex). Value (Set) = String.format("%d.%d", testIndex, actionIndex) [0..*] */
            testScriptDebuggerWebSocket: null,
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

            // Sync showDebugButton because Vue does not support reactivity on Map or Set

            // if (obj.testScriptIndex in state.showDebugButton === false) {
                Vue.set(state.showDebugButton, obj.testScriptIndex, {breakpointIndex: null, debugButtonLabel: "Debug"}) // Add property using Vue.set to nudge reactivity
                console.log(obj.testScriptIndex + "added" + obj.breakpointIndex)
            // } else {
            //     if (state.showDebugButton[obj.testScriptIndex] === false) {
            //         Vue.set(state.showDebugButton, obj.testScriptIndex, true)
            //     }
            // }
        },
        removeBreakpoint(state, obj) {
            if (state.breakpointMap.has(obj.testScriptIndex)) {
                var breakpointSet = state.breakpointMap.get(obj.testScriptIndex)
                if (breakpointSet.has(obj.breakpointIndex)) {
                    breakpointSet.delete(obj.breakpointIndex)

                    if (breakpointSet.size == 0) {
                        if (obj.testScriptIndex in state.showDebugButton === true) {
                            // Vue.set(state.showDebugButton, obj.testScriptIndex, false) // Add property using Vue.set to nudge reactivity
                            Vue.delete(state.showDebugButton, obj.testScriptIndex)
                            // console.log(obj.testScriptIndex + "removed" + obj.breakpointIndex)
                        }
                    }
                }
            }
        },
        setDebugAction(state, obj) {
            if (obj.testScriptIndex in state.showDebugButton) {
                let valObj = state.showDebugButton[obj.testScriptIndex]
                if (valObj != undefined) {
                    valObj.breakpointIndex = obj.breakpointIndex
                    valObj.debugButtonLabel = obj.debugButtonLabel // "Resume"
                }
            } else {
                alert(' failed: ' + obj.testScriptIndex)
            }
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
        isBreakpointHit: (state) => (obj) => {
            if (obj.testScriptIndex in state.showDebugButton) {
                let valObj = state.showDebugButton[obj.testScriptIndex]
                if (valObj != undefined) {
                    if (valObj.breakpointIndex === obj.breakpointIndex) {
                        return true
                    }
                }
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
        getActivelyDebuggingTestScriptIndex: state => {
           for (let key in state.showDebugButton) {
               let valObj = state.showDebugButton[key]
               if (valObj != undefined && 'debugButtonLabel' in valObj) {
                   if ('Resume' === valObj.debugButtonLabel) {
                       return key
                   }
               }
           }
           console.log('Active debug testScriptIndex not found!')
           return null
        }
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
        async debugKill({commit, rootState, state}, mapKey) {
            if (state.testScriptDebuggerWebSocket != null) {
                state.testScriptDebuggerWebSocket.send('{"killDebug":"true"}')
                // let valObj = state.showDebugButton[mapKey]
                // if (valObj != undefined) {
                //     console.log('Killing from ' + valObj.breakpointIndex)
                //     valObj.breakpointIndex = null
                //     valObj.debugButtonLabel = "Debug"
                // }
            } else {
                console.log('debugKill failed: WebSocket is null!')
            }
        },
        async debugTestScript({commit, rootState, state, getters}, testId) {
            console.log('in debug' + testId)
            // commit('setTestReport',{name: testId, testReport: null})
            // console.log('log cleared for ' + testId)

            commit('setCurrentTest', testId)
            console.log('pass 2')

            let indexOfTestId = this.state.testRunner.testScriptNames.indexOf(testId)
            let testCollectionName = this.state.testRunner.currentTestCollectionName
            const testCollectionIndex = this.state.testRunner.serverTestCollectionNames.indexOf(testCollectionName)
            const mapKey = testCollectionIndex + '.' + indexOfTestId

            // Technically it is possible to run many debugger web sockets but the test display only allows "opening" one active Test bar at a time since the previous test bar is automatically closed.

            if (state.testScriptDebuggerWebSocket === null) {
                state.testScriptDebuggerWebSocket = new WebSocket('wss://fhirtoolkit.test:9743/asbestos/testScriptDebugger') // TODO: Replace https off the HTTPS TOOLKIT BASE and append the Endpoint
                state.testScriptDebuggerWebSocket.onopen = event => {
                    // Disable Run button
                    console.log('In socket onOpen. event: ' + event === undefined)
                    // clear log 1?
                    commit('clearTestReports')
                    let uri = `debug-testscript/${rootState.base.session}__${rootState.base.channelId}/${testCollectionName}/${testId}?_format=${this.state.testRunner.useJson ? 'json' : 'xml'};_gzip=${this.state.testRunner.useGzip}`
                    if (indexOfTestId > -1) {
                        const breakpointSet = state.breakpointMap.get(mapKey) // Follow proper key format
                        let breakpointArrayString = JSON.stringify([...breakpointSet])
                        let sendData = `{"uri":"${uri}","testScriptIndex":"${mapKey}","breakpointList":${breakpointArrayString}}`
                        console.log('Sending: ' + sendData)
                        state.testScriptDebuggerWebSocket.send(sendData)
                    } else {
                        console.log(indexOfTestId + ": index not found for testId:  " + testId)
                    }
                }
                state.testScriptDebuggerWebSocket.onclose = event => {
                    // Enable Run button
                    if (event != null && event != undefined) {
                        console.log('onclose data: ' + event.returnData)
                    }
                    let actionData = {testScriptIndex: this.getters.getActivelyDebuggingTestScriptIndex, breakpointIndex: null, debugButtonLabel: 'Debug'}
                    commit('setDebugAction', actionData)
                    console.log('In socket onClose. Setting socket to null...')
                    state.testScriptDebuggerWebSocket = null
                    console.log('done.')
                }
                state.testScriptDebuggerWebSocket.onmessage = event => {
                    console.log('onMessage: ' + event.data)
                    let returnData = JSON.parse(event.data)
                    if (returnData.messageType === 'final-report') {
                        commit('setCombinedTestReports', returnData.testReport)
                        state.testScriptDebuggerWebSocket.close()
                    } else if (returnData.messageType === 'breakpoint-hit') {
                        // clear log 2?
                       console.log('breakpoint hit: ' + returnData.breakpointIndex) //  This needs to be {key: x, breakpointIndex: x}
                       commit('setDebugAction', returnData)
                       commit('setCombinedTestReports', returnData.testReport)
                    } else if (returnData.messageType === 'killed') {
                        alert('Debug: Killed')
                        state.testScriptDebuggerWebSocket.close()
                    } else if (returnData.messageType === 'unexpected-error') {
                        alert('Debug: Unexpected error.')
                        state.testScriptDebuggerWebSocket.close()
                    }
                }
                state.testScriptDebuggerWebSocket.onerror = function(event) {
                    if (event != null && event != undefined) {
                        alert('Error: ' + event.data)
                    }
                }

            } else if (mapKey in state.showDebugButton && state.showDebugButton[mapKey].debugButtonLabel === 'Resume') {
                console.log('Resuming from ' + state.showDebugButton[mapKey].breakpointIndex)
                state.testScriptDebuggerWebSocket.send('{"resumeBreakpoint":"true"}')
                state.showDebugButton[mapKey].breakpointIndex = null // Clear flag
            }
        },
    }
}
