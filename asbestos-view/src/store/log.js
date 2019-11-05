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
        loadEventSummaries({commit, rootState}) {
            if (!rootState.base.session) {
                this.$store.commit('setError', 'Session not set in logStore.loadEventSummaries')
                console.error('Session not set')
                return
            }
            if (!rootState.base.channelId) {
                this.$store.commit('setError', 'Channel not set in logStore.loadEventSummaries')
                console.error('Channel not set')
                return
            }
            LOG.get(`${rootState.base.session}/${rootState.base.channelId}`, {
                params: {
                    summaries: 'true'
                }
            })
                .then(response => {
                    const eventSummaries = response.data.sort((a, b) => {
                        if (a.eventName < b.eventName) return 1
                        return -1
                    })
                    commit('setEventSummaries', eventSummaries)
                })
                .catch(error => {
                    this.$store.commit('setError', error)
                    console.error(error)
                })
        },
    }
}
