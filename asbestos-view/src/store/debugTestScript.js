import Vue from "vue";
import Vuex from "vuex";
import {UtilFunctions} from "../common/http-common";

Vue.use(Vuex)

export const debugTestScriptStore = {
    state() {
        return {
            waitingForBreakpoint: false,
            evalMode: false,
            /* keyProperty{testScriptIndex}=value{breakpointIndex: this is only used for the breakpoint hit index, debugButtonLabel: ''}
             debugButtonLabel exists inside of showDebugButton because there are multiple testscripts and it is necessary to keep showing the Debug button labels for other test scripts.
            If a single button label variable was used then all of the Debug buttons would be become changed to Resume when a breakpoint is hit */
            showDebugButton: {},
            /* Key = String.format("%d.%d", testCollectionIndex , testScriptIndex).
            Value (Set) = String.format("%d.%d", testIndex, actionIndex) [0..*] */
            breakpointMap: new Map(),
            testScriptDebuggerWebSocket: null,
            debugMgmtWebSocket: null,
            debugMgmtIndexList: [],
            doCleanupBreakpoints: function(state, fqTestScriptIndex) {
                if (fqTestScriptIndex in state.showDebugButton === true) {
                    // Vue.set(state.showDebugButton, obj.testScriptIndex, false) // Add property using Vue.set to nudge reactivity
                    let valObj = state.showDebugButton[fqTestScriptIndex]
                    if (valObj !== undefined) {
                        if (valObj.debugButtonLabel === 'Debug') { // Only remove when Debug hasn't started yet
                            Vue.delete(state.showDebugButton, fqTestScriptIndex)
                        }
                    }
                    // console.log(obj.testScriptIndex + "removed" + obj.breakpointIndex)
                }
            },
            reapplyBkptChange: function(state, obj) {
                let valObj = state.showDebugButton[obj.testScriptIndex]
                if (valObj !== null && valObj !== undefined) {
                    Vue.set(state.showDebugButton, obj.testScriptIndex, {breakpointIndex: valObj.breakpointIndex, debugButtonLabel: valObj.debugButtonLabel}) // set the same value to nudge reactivity
                }
            },
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
            console.log(obj.testScriptIndex + " added " + obj.breakpointIndex)

            // Sync showDebugButton because Vue does not support reactivity on Map or Set

            if (obj.testScriptIndex in state.showDebugButton === false) { // Only add it the first time where the showDebugButton object is missing the scriptIndex property
                Vue.set(state.showDebugButton, obj.testScriptIndex, {breakpointIndex: null, debugButtonLabel: "Debug"}) // Add property using Vue.set to nudge reactivity
            } else {
                state.reapplyBkptChange(state, obj)
            }
        },
        removeBreakpoint(state, obj) {
            if (state.breakpointMap.has(obj.testScriptIndex)) {
                let breakpointSet = state.breakpointMap.get(obj.testScriptIndex)
                if (breakpointSet.has(obj.breakpointIndex)) {
                    breakpointSet.delete(obj.breakpointIndex)

                    if (breakpointSet.size == 0) {// When all breakpoints were removed while in Resume activity, then still allow to Resume so when breakpointList is empty
                        state.doCleanupBreakpoints(state, obj.testScriptIndex)
                    } else {
                        let valObj = state.showDebugButton[obj.testScriptIndex]
                        if (valObj != undefined) {
                            Vue.set(state.showDebugButton, obj.testScriptIndex, {breakpointIndex: valObj.breakpointIndex, debugButtonLabel: valObj.debugButtonLabel}) // set the same value to nudge reactivit
                        }
                    }
                }
            }
        },
        removeAllBreakpoints(state, obj) {
            if (state.breakpointMap.has(obj.testScriptIndex)) {
                let breakpointSet = state.breakpointMap.get(obj.testScriptIndex)
                if (breakpointSet !== null && breakpointSet !== undefined) {
                    breakpointSet.clear()
                    Vue.delete(state.breakpointMap, obj.testScriptIndex)
                    // Vue.set(state.breakpointMap, obj.testScriptIndex, new Set())
                    state.doCleanupBreakpoints(state, obj.testScriptIndex)
                    state.reapplyBkptChange(state, obj)
                }
            }
        },
        setDebugButtonLabel(state, obj) {
            if (obj.testScriptIndex in state.showDebugButton) {
                let valObj = state.showDebugButton[obj.testScriptIndex]
                if (valObj !== undefined) {
                    valObj.breakpointIndex = obj.breakpointIndex
                    valObj.debugButtonLabel = obj.debugButtonLabel // "Resume"
                }
            } else {
                alert(' failed: ' + obj.testScriptIndex)
            }
        },
        setBeingDebuggedList(state, ar) {
            if (Array.isArray(ar)) {
                state.debugMgmtIndexList = ar
            }
        }
    },
    getters: {
        hasBreakpoint: (state) => (obj) => {
            if (state.breakpointMap.has(obj.testScriptIndex)) {
                const breakpointSet = state.breakpointMap.get(obj.testScriptIndex)
                if (breakpointSet) {
                    const retVal = breakpointSet.has(obj.breakpointIndex)
                    return retVal
                }
            }
            return false
        },
        hasBreakpoints: (state) => (testScriptIndex) => {
            if (state.breakpointMap.has(testScriptIndex)) {
                const breakpointSet = state.breakpointMap.get(testScriptIndex)
                if (breakpointSet) {
                    return breakpointSet.size > 0
                }
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
           console.log('Active debug testScriptIndex not found!') // Possible that breakpoint was never hit from the backend at all for this to happen
           return null
        },
        getIndexOfTestId: (state, getters, rootState) => (testId) => {
            let indexOfTestId = rootState.testRunner.testScriptNames.indexOf(testId)
            return indexOfTestId
        },
        getMapKey: (state, getters, rootState)  => (testId) => {
            let testCollectionName = rootState.testRunner.currentTestCollectionName
            const testCollectionIndex = rootState.testRunner.serverTestCollectionNames.indexOf(testCollectionName)
            const mapKey = testCollectionIndex + '.' + getters.getIndexOfTestId(testId)
            return mapKey
        },
        getCurrentMapKey: (getters, rootState) => {
            const testId = rootState.testRunner.currentTest
            const mapKey = getters.getMapKey(testId)
            return mapKey
        }
    },
    actions: {
        addBreakpoint({commit}, value) {
            if (value != null && value != undefined) {
                if (value.testScriptIndex != null && value.testScriptIndex != undefined) {
                    if (value.breakpointIndex != null && value.breakpointIndex != undefined) {
                        commit('addBreakpoint', value)
                        return true
                    }
                }
            }
            alert('Requested breakpoint could not be added.')
            return false
        },
        removeBreakpoint({commit}, value) {
            commit('removeBreakpoint', value)
            return true
        },
        async removeAllBreakpoints({commit}, value) {
            commit('removeAllBreakpoints', value)
            return true
        },
        async stopDebugTs({state}, mapKey) {
            if (state.testScriptDebuggerWebSocket != null) {
                state.testScriptDebuggerWebSocket.send('{"cmd":"stopDebug"}')
                // let valObj = state.showDebugButton[mapKey]
                // if (valObj != undefined) {
                //     console.log('Killing from ' + valObj.breakpointIndex)
                //     valObj.breakpointIndex = null
                //     valObj.debugButtonLabel = "Debug"
                // }
            } else {
                console.log('stopDebugTs ' + mapKey + ' failed: WebSocket is null!')
            }
        },
        async doDebugEvalMode({commit, state, rootState, getters}) {
            let testId = rootState.testRunner.currentTest
            console.log('In doDebugEvalMode: ' + testId)
            const mapKey = getters.getMapKey(testId)
            const breakpointIndex = state.showDebugButton[mapKey].breakpointIndex
            if (rootState.debugAssertionEval.assertionEvalBreakpointIndex === breakpointIndex) {
                commit('setShowDebugEvalModal', true)
            } else {
                commit('setAssertionEvalBreakpointIndex', breakpointIndex)
                let sendData = `{"cmd":"requestOriginalAssertion","testScriptIndex":"${mapKey}"}`

                console.log('Requesting original-assertion ' + breakpointIndex)
                state.testScriptDebuggerWebSocket.send(sendData)
            }
        },
        async doDebugEvalAssertion({state}, assertionDataBase64) {
            let sendData = `{"cmd":"debugEvalAssertion", "base64String":"${assertionDataBase64}"}`
            state.testScriptDebuggerWebSocket.send(sendData)
        },
        async debugTestScript({commit, rootState, state, getters, dispatch}, testId) {
            console.log('in debug' + testId + ' isGettersUndefined: ' + (getters === undefined).valueOf())
            // commit('setTestReport',{name: testId, testReport: null})
            // console.log('log cleared for ' + testId)

            commit('setCurrentTest', testId)
            const mapKey = this.getters.getMapKey(testId)
            const testSessionId = rootState.base.session

            // Technically it is possible to run many debugger web sockets but the test display only allows "opening" one active Test bar at a time since the previous test bar is automatically closed.

            if (state.testScriptDebuggerWebSocket === null) {
                const wssBase = UtilFunctions.getWssBase()
                const channelId = rootState.base.channelId
                const wssSocketUrl = `${wssBase}/debugTestScript/developer?ftkTestSessionId=${testSessionId}&channelId=${channelId}&testScriptIndex=${mapKey}`
                state.testScriptDebuggerWebSocket = new WebSocket(wssSocketUrl)
                state.testScriptDebuggerWebSocket.onopen = event => {
                    state.waitingForBreakpoint = true
                    // Disable Run button
                    console.log('In socket onOpen. event: ' + (event === undefined).valueOf())
                    // clear log 1?
                    commit('clearTestReports')
                    let uri = `debug-testscript/${testSessionId}__${channelId}/${this.state.testRunner.currentTestCollectionName}/${testId}?_format=${this.state.testRunner.useJson ? 'json' : 'xml'};_gzip=${this.state.testRunner.useGzip}`
                    let indexOfTestId = getters.getIndexOfTestId(testId)
                    if (indexOfTestId > -1) {
                        const breakpointSet = state.breakpointMap.get(mapKey) // Follow proper key format
                        let breakpointArrayString = JSON.stringify([...breakpointSet])
                        let sendData = `{"cmd":"beginDebug","uri":"${uri}","testScriptIndex":"${mapKey}","breakpointList":${breakpointArrayString}}`
                        // console.log('Sending: ' + sendData)
                        state.testScriptDebuggerWebSocket.send(sendData)
                    } else {
                        // console.log(indexOfTestId + ": index not found for testId:  " + testId)
                        return
                    }
                }
                state.testScriptDebuggerWebSocket.onclose = event => {
                    commit('setAssertionEvalBreakpointIndex', '') // Reset the Eval state on Close so original assertion is requested next time it is run
                    state.waitingForBreakpoint = false
                    // Enable Run button
                    if (event != null && event != undefined) {
                        // console.log('onclose data: ' + event.returnData)
                    }
                    // Breakpoints could have been cleared so only reset the label to Debug
                    let actionData = {
                        testScriptIndex: this.getters.getActivelyDebuggingTestScriptIndex,
                        breakpointIndex: null,
                        debugButtonLabel: 'Debug'
                    }
                    commit('setDebugButtonLabel', actionData)
                    if (! getters.hasBreakpoints(actionData.testScriptIndex)) {
                        state.doCleanupBreakpoints(state, actionData.testScriptIndex)
                    }
                    // console.log('In socket onClose. Setting socket to null...')
                    state.testScriptDebuggerWebSocket = null
                    // console.log('done.')
                }
                state.testScriptDebuggerWebSocket.onmessage = event => {
                    let messageStrLimit = 500
                    if (event && event.data) {
                        console.log('onMessage: ' + (event.data.length < messageStrLimit ?
                            event.data
                            : 'Message too long. Showing first ' + messageStrLimit + " characters: " + event.data.substr(0, messageStrLimit) + ' TRUNCATED.'))
                    } else {
                        console.log('event data is missing!')
                    }
                    let returnData = JSON.parse(event.data)
                    if (returnData.messageType === 'final-report') {
                        state.waitingForBreakpoint = false
                        commit('setShowDebugEvalModal', false) // If this is not here, the modal will automatically re-open when the test script is debugged again immediately
                        commit('setCombinedTestReports', returnData.testReport)
                        state.testScriptDebuggerWebSocket.close()
                    } else if (returnData.messageType === 'breakpoint-hit') {
                        state.waitingForBreakpoint = false
                        // clear log 2?
                        // console.log('breakpoint hit: ' + returnData.breakpointIndex) //  This needs to be {key: x, breakpointIndex: x}
                        commit('setDebugButtonLabel', returnData)
                        commit('setCombinedTestReports', returnData.testReport)
                        if (('isEvaluable' in returnData) && returnData.isEvaluable === 'true') {
                            state.evalMode = true
                            if (rootState.debugAssertionEval.showModal === true) {
                                // Auto-refresh the modal if already evalMode is already displaying the modal
                                //  state.doDebugEvalMode({commit: commit, state: state, rootState: rootState, getters: getters}, testId)
                                dispatch('doDebugEvalMode')
                            }
                        }
                    } else if (returnData.messageType === 'original-assertion') {
                        // alert(JSON.stringify(returnData.assertionJson))
                        // rootState.testScriptAssertionEval.
                        commit('updateAssertionEvalObj', returnData.assertionJson)
                    } else if (returnData.messageType === 'eval-assertion-result') {
                        commit('setDebugAssertionEvalResult', returnData)
                    } else if (returnData.messageType === 'stoppedDebugging') {
                        // alert('Debugging was stopped.')
                        state.testScriptDebuggerWebSocket.close()
                    } else if (returnData.messageType === 'unexpected-error') {
                        alert('Debug: Unexpected error.')
                        state.testScriptDebuggerWebSocket.close()
                    }
                }
                state.testScriptDebuggerWebSocket.onerror = function (event) {
                    state.waitingForBreakpoint = false
                    if (event != null && event != undefined) {
                        alert('Error: ' + event.data)
                    }
                }
            } else {
                // The Debug button changes to Resume when the socket is active, so one button does two jobs.
                const breakpointSet = state.breakpointMap.get(mapKey) // Follow proper key format
                let breakpointArrayString = JSON.stringify([...breakpointSet])
                dispatch('doResumeBreakpoint', breakpointArrayString)
            }
        },
        async doResumeBreakpoint({rootState, state}, breakpointArrayString) {
            const testId = rootState.testRunner.currentTest
            if (state.testScriptDebuggerWebSocket === null) {
                console.log('doResumeBreakpoint: socket is null!')
                return
            }
            const mapKey = this.getters.getMapKey(testId)
            if (mapKey in state.showDebugButton && state.showDebugButton[mapKey].debugButtonLabel === 'Resume') {
                state.evalMode = false
                state.waitingForBreakpoint = true
                let sendData = `{"cmd":"resumeBreakpoint","testScriptIndex":"${mapKey}","breakpointList":${breakpointArrayString}}`

                // console.log('Resuming from ' + state.showDebugButton[mapKey].breakpointIndex)
                state.testScriptDebuggerWebSocket.send(sendData)
                state.showDebugButton[mapKey].breakpointIndex = null // Clear flag
            }
        },
        async doStepOver({dispatch}) {
           dispatch('doResumeBreakpoint', '["stepOverBkpt"]')
        },
        async debugMgmt({commit, rootState, state}, fn) {
            if (fn === undefined || fn === null )
                return
            if (state.debugMgmtWebSocket === null) {
                const wssBase = UtilFunctions.getWssBase()
                const wssSocketUrl = `${wssBase}/debugTestScript/developer`
                state.debugMgmtWebSocket = new WebSocket(wssSocketUrl)
                state.debugMgmtWebSocket.onopen = event => {
                    if (event === false) {
                        console.log('In debugMgmt socket onOpen. event: ' + (event === undefined).valueOf())
                    }
                    const testSessionId = rootState.base.session
                    const channelId = rootState.base.channelId
                    const cmd = fn.cmd
                    if ('getExistingDebuggerList' === cmd) {
                        const sendData = `{"cmd":"${cmd}","ftkTestSessionId":"${testSessionId}","channelId":"${channelId}"}`
                        state.debugMgmtWebSocket.send(sendData)
                    } else if ('removeDebugger' === cmd) {
                        const sendData = `{"cmd":"${cmd}","ftkTestSessionId":"${testSessionId}","channelId":"${channelId}","testScriptIndex":"${fn.testScriptIndex}"}`
                        state.debugMgmtWebSocket.send(sendData)
                    }
                }
                state.debugMgmtWebSocket.onmessage = event => {
                    // console.log('debugMgmt onMessage: ' + (event.data))
                    let returnData = JSON.parse(event.data)
                    const messageType = returnData.messageType
                    if (messageType === 'existingDebuggersList' || messageType === 'removedDebugger') {
                        if (returnData.indexList && returnData.indexList.length > 0) {
                            commit('setBeingDebuggedList', returnData.indexList)
                        } else {
                            commit('setBeingDebuggedList', [])
                        }
                    }
                    state.debugMgmtWebSocket.close()
                    state.debugMgmtWebSocket = null
                }
                state.debugMgmtWebSocket.onerror = function (event) {
                    if (event != null && event != undefined) {
                        alert('debugMgmt Error: ' + event.data)
                    }
                }
                state.debugMgmtWebSocket.onclose = event => {
                    if (event != null && event != undefined) {
                        // console.log('debugMgmt onclose data: ' + event.returnData)
                    }
                }
            }


        },
    },
}
