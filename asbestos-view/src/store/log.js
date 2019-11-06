import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

import {LOG} from '../common/http-common'

export const logStore = {
    state() {
        return {
            // private to the log viewer
            eventSummaries: [],  // list { eventName: xx, resourceType: yy, verb: GET|POST, status: true|false }
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
        selectEvent(state, eventName) {
            const index = state.eventSummaries.findIndex(eventSummary => {
                return eventSummary.eventName === eventName
            })
            if (index !== -1)
                state.currentEventIndex = index
            console.log(`new event index is ${index}`)
        }
    },
    actions: {
        async loadEventSummaries({commit, rootState}) {
            if (!rootState.base.session) {
                commit('setError', 'Session not set in logStore.loadEventSummaries')
                console.error('Session not set')
                return
            }
            if (!rootState.base.channelId) {
                commit('setError', 'Channel not set in logStore.loadEventSummaries')
                console.error('Channel not set')
                return
            }
            try {
                const rawSummaries = await LOG.get(`${rootState.base.session}/${rootState.base.channelId}`, {
                    params: {
                        summaries: 'true'
                    }
                })

                const eventSummaries = rawSummaries.data.sort((a, b) => {
                    if (a.eventName < b.eventName) return 1
                    return -1
                })
                commit('setEventSummaries', eventSummaries)
            } catch (error) {
                commit('setError', error)
                console.error(error)
            }
        },
    }
}
