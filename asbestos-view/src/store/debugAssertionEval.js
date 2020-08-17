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

function EvalResultObj() {
    this.propKey = ''
    this.resultMessage = ''
    this.markdownMessage = ''
}

export const debugAssertionEvalStore = {
    state() {
        return {
            assertionEvalBreakpointIndex: '',
            showModal: false,
            isEvalObjUpdated: false,
            // debugAssertionEvalResult: {propKey: '', resultMessage: '', markdownMessage: ''},
            // evalObj: new EvalAssertionObj(),
            evalObjByPattern: {
                    patternTypes:
                        {
                            originalAssertion: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj(), displayFieldList: ['compareToSourceId','warningOnly']},
                            compareToSourceId: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj(), displayFieldList: ['compareToSourceId','compareToSourceExpression','warningOnly']},
                        }
                    },
            collapsibleDisplayEventObj: {displayOpen: false, breakpointObj: null},
            fieldValueTypes : null,
        }
    },
    mutations: {
        setAssertionEvalBreakpointIndex(state, val) {
         state.assertionEvalBreakpointIndex = val
        },
        setEvalObjProperty(state, obj) {
            let dataRef = state.evalObjByPattern.patternTypes[obj.patternTypeId].dataObj
            if ('propKey' in obj) {
                if (obj.propKey in dataRef) {
                    dataRef[obj.propKey] = obj.propVal
                }
            }
        },
        setShowDebugEvalModal(state, bVal) {
            state.showModal = Boolean(bVal)
        },
        setDebugAssertionEvalPropKey(state, obj) {
            let resultRef = state.evalObjByPattern.patternTypes[obj.patternTypeId].resultObj
            resultRef.propKey = obj.propKey
        },
        setDebugAssertionEvalResult(state, obj) {
            let resultRef = state.evalObjByPattern.patternTypes[obj.patternTypeId].resultObj
            resultRef.resultMessage = obj.resultMessage
            if ('exceptionPropKey' in obj) {
                resultRef.propKey = obj.exceptionPropKey
            } else {
                resultRef.propKey = ''
            }
            let mkdwnMsg = ''
            if ('markdownMessage' in obj) {
                if (obj.markdownMessage.valueOf().length > 0) {
                    mkdwnMsg = window.atob(obj.markdownMessage)
                }
            }
            resultRef.markdownMessage = mkdwnMsg
        },
        updateAssertionEvalObj(state, obj) {
           let atLeastOnePropertyWasUpdated = false
           let dataRef = state.evalObjByPattern.patternTypes['originalAssertion'].dataObj
            for (let propKey in dataRef) {
             if (propKey in obj) {
                 if (typeof obj[propKey] === 'string') {
                     dataRef[propKey] = obj[propKey]
                 } else if ('myStringValue' in obj[propKey]) {
                    dataRef[propKey] = obj[propKey].myStringValue
                 }
                 if (atLeastOnePropertyWasUpdated === false) {
                     atLeastOnePropertyWasUpdated = true
                     // console.log('propKey: ' + propKey + ' was set to: ' + state.evalObj[propKey] + '. inprop? ' + Boolean('myStringValue' in obj[propKey])) // obj[propKey].myStringValue
                 }
             } else {
                 dataRef[propKey] = ''
             }
            }
           if (atLeastOnePropertyWasUpdated) {
              state.isEvalObjUpdated = true
           }
           state.showModal = true
        },
        setFieldValueTypes(state, obj /* object containing an array of other value types */) {
           state.fieldValueTypes = obj
        },
    },
    actions: {
    },
}
