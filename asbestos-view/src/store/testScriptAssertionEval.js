import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex)

export const testScriptAssertionEvalStore = {
    state() {
        return {
            displayEvalModal: false,
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
        copyAssertionEvalObj(state, obj) {
           for (let propKey in state.evalObj) {
             if (propKey in obj) {
                 state.evalObj[propKey] = obj[propKey]
             }
            }
        },
    },
    actions: {},
}
