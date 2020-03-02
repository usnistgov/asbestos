import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

import {VALIDATE} from '../common/http-common'

export const logStore = {
    state() {
        return {
            operationOutcome: null,
        }
    },
    mutations: {
        setOperationOutcome(state, outcome) {
            state.operationOutcome = outcome
        },
    },
    getters: {

    },
    actions: {
        async getValidation({commit}, parms) {
            const resourceType = parms.resourceType
            const urlOfResource = parms.url
            const gzip = parms.gzip
            const useProxy = parms.useProxy
            try {
                const result = await VALIDATE.post(`${resourceType}?url=${urlOfResource};gzip=${gzip};useProxy=${useProxy}`)
                commit('setOperationOutcome', result.data)
            } catch (error) {
                commit('setError', error)
                console.error(error)
            }
        }
    }

}
