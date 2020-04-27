import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex)

export const debugAssertionEvalStore = {
    state() {
        return {
            showModal: false,
            isEvalObjUpdated: false,
            evalObj: {
                label: '',
                description: '',
                direction: '',
                compareToSourceId: '',
                compareToSourceExpression: '',
                compareToSourcePath: '',
                contentType: '',
                expression: '',
                headerField: '',
                minimumId: '',
                navigationLinks: '',
                operator: '',
                path: '',
                requestMethod: '',
                requestURL: '',
                resource: '',
                response: '',
                responseCode: '',
                sourceId: '',
                validateProfileId: '',
                value: '',
                warningOnly: '',
            },
        }
    },
    mutations: {
        setShowDebugEvalModal(state, bVal) {
            state.showModal = Boolean(bVal)
        },
        updateAssertionEvalObj(state, obj) {
           let atLeastOnePropertyWasUpdated = false
           for (let propKey in state.evalObj) {
             if (propKey in obj) {
                 state.evalObj[propKey] = obj[propKey]
                 if (atLeastOnePropertyWasUpdated === false) {
                     atLeastOnePropertyWasUpdated = true
                 }
             }
            }
           if (atLeastOnePropertyWasUpdated) {
              state.isEvalObjUpdated = true
           }
           state.showModal = true
        },
    },
    actions: {},
}
