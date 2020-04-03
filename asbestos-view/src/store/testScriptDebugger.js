import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex)

export const testScriptDebuggerStore = {
    state() {
        return {
            showDebugButton: {},
            breakpointMap: new Map(),
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
                // console.log(obj.testScriptIndex + "added" + obj.breakpointIndex)
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
    },
    getters: {
        hasBreakpointsForTestScriptId: (state) => (id) => {
            return state.breakpointMap.has(id)
        }
    },
    actions: {
        addBreakpoint({commit}, value) {
            commit('addBreakpoint', value)
        },
        removeBreakpoint({commit}, value) {
            commit('removeBreakpoint', value)
        },
    }
}
