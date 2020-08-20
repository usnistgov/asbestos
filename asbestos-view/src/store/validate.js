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
            const url = `${resourceType}?url=${urlOfResource};gzip=${gzip};useProxy=${useProxy}`
            try {
                const result = await VALIDATE.post(url)
                commit('setOperationOutcome', result.data)
            } catch (error) {
                commit('setError', `${error} for ${url}`)
                console.error(`${error} for ${url}`)
            }
        }
    }

}
