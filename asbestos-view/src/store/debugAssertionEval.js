import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex)

function EvalAssertionObj() {
    /*
    When the original assertion properties are initially requested from the backend, empty properties are not included by JSON serialization.
    Since this field-list is used to auto create the input text boxes, a full representation is missing. A complete list is required
    to be known on the client side.
    */
    this.label = ''
    this.description = ''
    this.direction = ''
    this.compareToSourceId = ''
    this.compareToSourceExpression = ''
    this.compareToSourcePath = ''
    this.contentType = ''
    this.expression = ''
    this.headerField = ''
    this.minimumId = ''
    this.navigationLinks = ''
    this.operator = ''
    this.path = ''
    this.requestMethod = ''
    this.requestURL = ''
    this.resource = ''
    this.response = ''
    this.responseCode = ''
    this.sourceId = ''
    this.validateProfileId = ''
    this.value = ''
    this.warningOnly = ''
}

export const debugAssertionEvalStore = {
    state() {
        return {
            assertionEvalBreakpointIndex: '',
            showModal: false,
            isEvalObjUpdated: false,
            debugAssertionEvalResult: {propKey: '', resultMessage: '', markdownMessage: ''},
            evalObj: new EvalAssertionObj(),
            evalObjTODO: {
                    patternType:
                        {
                            genericType: {dataObj: new EvalAssertionObj()},
                            compareToSourceId: {dataObj: new EvalAssertionObj(), fieldList: ['compareToSourceId','compareToSourceExpression','warningOnly']}
                        }
                    },
            collapsibleDisplayEventObj: {displayOpen: false, breakpointObj: null},
            enumValueTypes : null,
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
            if ('exceptionPropKey' in obj) {
                state.debugAssertionEvalResult.propKey = obj.exceptionPropKey
            } else {
                state.debugAssertionEvalResult.propKey = ''
            }
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
        setEnumValueTypes(state, obj /* object containing an array of other value types */) {
           state.enumValueTypes = obj
        },
    },
    actions: {
    },
}
