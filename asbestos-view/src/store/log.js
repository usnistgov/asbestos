import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export const logStore = {
    state() {
        return {
            // private to the log viewer
            eventSummaries: null,
            currentEventIndex: 0,
        }
    },
    mutations: {
        setEventSummaries(state, summaries) {
            state.eventSummaries = summaries
        },
        updateCurrentEventIndex(state, value) {
            state.currentEventIndex += value
        },
        setCurrentEventIndex(state, index) {
            state.currentEventIndex = index
        },
    }
}
