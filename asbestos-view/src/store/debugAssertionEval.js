import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex)

export const debugAssertionEvalStore = {
    state() {
        return {
            assertionEvalBreakpointIndex: '',
            showModal: false,
            isEvalObjUpdated: false,
            debugAssertionEvalResult: {propKey: '', resultMessage: '', markdownMessage: ''},
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
        setAssertionEvalBreakpointIndex(state, val) {
         state.assertionEvalBreakpointIndex = val
        },
        setEvalObjProperty(state, obj) {
            if ('propKey' in obj) {
                if (obj.propKey in state.evalObj) {
                    state.evalObj[obj.propKey] = obj.propVal
                }
            }
        },
        setShowDebugEvalModal(state, bVal) {
            state.showModal = Boolean(bVal)
        },
        setDebugAssertionEvalPropKey(state, key) {
            state.debugAssertionEvalResult.propKey = key
        },
        setDebugAssertionEvalResult(state, obj) {
           state.debugAssertionEvalResult.resultMessage = obj.resultMessage
            let mkdwnMsg = ''
            if ('markdownMessage' in obj) {
                if (obj.markdownMessage.valueOf().length > 0) {
                    mkdwnMsg = window.atob(obj.markdownMessage)
                }
            }
            state.debugAssertionEvalResult.markdownMessage = mkdwnMsg
        },
        updateAssertionEvalObj(state, obj) {
           let atLeastOnePropertyWasUpdated = false
           for (let propKey in state.evalObj) {
             if (propKey in obj) {
                 if (typeof obj[propKey] === 'string') {
                     state.evalObj[propKey] = obj[propKey]
                 } else if ('myStringValue' in obj[propKey]) {
                    state.evalObj[propKey] = obj[propKey].myStringValue
                 }
                 if (atLeastOnePropertyWasUpdated === false) {
                     atLeastOnePropertyWasUpdated = true
                     // console.log('propKey: ' + propKey + ' was set to: ' + state.evalObj[propKey] + '. inprop? ' + Boolean('myStringValue' in obj[propKey])) // obj[propKey].myStringValue
                 }
             } else {
                 state.evalObj[propKey] = ''
             }
            }
           if (atLeastOnePropertyWasUpdated) {
              state.isEvalObjUpdated = true
           }
           state.showModal = true
        },
    },
    actions: {
    },
}
