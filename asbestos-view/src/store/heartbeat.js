import Vue from "vue";
import Vuex from "vuex";
import {ENGINE} from "../common/http-common";

Vue.use(Vuex)

export const heartbeatStore = {
    state() {
        return {
            hapiIsAlive: false,
            hapiDetails: null,
            xdsIsAlive: false,
            xdsDetails: null,
        }
    },
    mutations: {
        setHapiIsAlive(state, value) {
            state.hapiIsAlive = value
        },
        setHapiDetails(state, value) {
            state.hapiDetails = value
        },
        setXdsIsAlive(state, value) {
            state.xdsIsAlive = value
        },
        setXdsDetails(state, value) {
            state.xdsDetails = value
        },
    },
    actions: {
        hapiHeartbeat({commit}) {
            const url = `hapiheartbeat`
            ENGINE.get(url)
                .then(response => {
                    commit('setHapiIsAlive', response.data.responding)
                    commit('setHapiDetails', response.data.addr)
                })
                .catch(() => {
                    commit('setHapiIsAlive', false)
                })
        },
        xdsHeartbeat({commit}) {
            const url = `xdsheartbeat`
            ENGINE.get(url)
                .then(response => {
                    commit('setXdsIsAlive', response.data.responding)
                    commit('setXdsDetails', response.data.addr)
                })
                .catch(() => {
                    commit('setXdsIsAlive', false)
                })
        }
    }
}
