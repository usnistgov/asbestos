import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex)

function XpathNotSupportedFooter() {
    this.message = 'Note: FHIR Toolkit does not support XPath/JSONPath.'
}

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
    this.warningOnly = 'false' // Default everywhere
}

function resetDataStateObj(obj, pristineObj) {
    for (let propKey in obj) {
        obj[propKey] = pristineObj[propKey]
    }
}

function EvalResultObj() {
    this.propKey = ''
    this.resultMessage = ''
    this.markdownMessage = ''
    this.wasEvaluatedAtleastOnce = false
}

export const debugAssertionEvalStore = {
    state() {
        return {
            assertionEvalBreakpointIndex: '',
            showEvalModalDialog: false,
            isEvalObjUpdated: false,
            selectedPatternTypeId: 'AllParameters', // Initial string contains default pattern
            defaultPatternTypeId: 'AllParameters',
            evalObjByPattern: {
                    patternTypes:
                        {
                            AllParameters: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj(), displayFieldList: ['label','description','direction'
                                    ,'compareToSourceId','compareToSourceExpression','compareToSourcePath','contentType','expression','headerField','minimumId',
                                    'navigationLinks','operator','path','requestMethod','requestURL','resource','response','responseCode','sourceId','validateProfileId','value','warningOnly']
                                    , footerList: [new XpathNotSupportedFooter()]
                                    },
                          /*
                            Asbestos Test Engine does not support the compareToSourcePath element part of the CompareToSourceId pattern type.
                           */
                            CompareToSourceId: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['compareToSourceId','compareToSourceExpression','warningOnly']
                                ,footerList: [new XpathNotSupportedFooter()] },
                            ContentType: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['contentType','warningOnly']},
                            Expression: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['sourceId','expression','warningOnly']
                                , footerList: [new XpathNotSupportedFooter()]},
                            ExpressionValue: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['sourceId','expression','value','warningOnly']
                                , footerList: [new XpathNotSupportedFooter()]},
                            FHIRPathExpression: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , resourceList: []
                                , displayFieldList: ['sourceId','expression','value','warningOnly']
                                , footerList: [new XpathNotSupportedFooter()]
                                , evalAction: 'doDebugEvalForResources'},
                            HeaderField: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['headerField','warningOnly']},
                            MinimumId: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['minimumId','warningOnly']},
                            RequestMethod: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['sourceId','requestMethod','warningOnly']
                                , footerList: [new XpathNotSupportedFooter()]
                                , applyDataObjDefaults: function(dataObj) {
                                    dataObj.requestMethod = 'get' // default to HTTP GET
                                }
                            },
                            ResourceType: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['resource','warningOnly']},
                            Response: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['sourceId','response','warningOnly']},
                            ResponseCode: {dataObj: new EvalAssertionObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['responseCode','operator','warningOnly']},
                        }
                    },
            collapsibleDisplayEventObj: {displayOpen: false, breakpointObj: null},
            fieldSupport: {
                         // Mostly static content
                        fieldValueTypes : null, // This is where the static FHIR enumerated values for the drop down controls is stored.
                        overrideFieldTypes: null, // used for dropdown values related to: resource, contentType
            },
            fixtureIds: null,
        }
    },
    mutations: {
        setAssertionEvalBreakpointIndex(state, val) {
            state.assertionEvalBreakpointIndex = val
        },
        setEvalObjProperty(state, obj) {
            if ('propKey' in obj) {
                let dataRef = state.evalObjByPattern.patternTypes[obj.patternTypeId].dataObj
                if (obj.propKey in dataRef) {
                    dataRef[obj.propKey] = obj.propVal
                }
            }
        },
        setShowDebugEvalModal(state, bVal) {
            state.showEvalModalDialog = Boolean(bVal)
        },
        setDebugAssertionEvalPropKey(state, obj) {
            let resultRef = state.evalObjByPattern.patternTypes[obj.patternTypeId].resultObj
            resultRef.propKey = obj.propKey
        },
        setSelectedPatternTypeId(state, patternTypeId) {
           state.selectedPatternTypeId = patternTypeId
        },
        setDebugAssertionEvalResult(state, obj) {
            try {
                let patternObj = state.evalObjByPattern.patternTypes[state.selectedPatternTypeId]
                let resultRef = patternObj.resultObj

                if (! resultRef.wasEvaluatedAtleastOnce) {
                    resultRef.wasEvaluatedAtleastOnce = true
                }
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
            } catch (e) {}

        },
        setEvalForResourcesResult(state, obj) {
            try {
                let patternObj = state.evalObjByPattern.patternTypes[state.selectedPatternTypeId]

                // Remove all array elements
                if (patternObj.resourceList.length > 0) {
                    patternObj.resourceList.splice(0, patternObj.resourceList.length)
                }

                // Reload elements from the result object into the pattern type
                if ('resourceList' in obj && 'resourceList' in patternObj) {
                    for (let r of obj.resourceList) {
                        patternObj.resourceList.push(r)
                    }
                }
            } catch (e) {}
        },
        updateAssertionEvalObj(state, obj) {
           let atLeastOnePropertyWasUpdated = false
            const defaultPatternTypeId = state.defaultPatternTypeId
            const patternObj = state.evalObjByPattern.patternTypes[defaultPatternTypeId]
            let dataRef = patternObj.dataObj

            // Reset the current selected pattern type to default
            state.selectedPatternTypeId = state.defaultPatternTypeId

            // Clear all existing pattern types' dataObj and resultObj before reloading original assertion
            for (let patternTypeName in state.evalObjByPattern.patternTypes) {
                let patternObj1 = state.evalObjByPattern.patternTypes[patternTypeName]
                resetDataStateObj(patternObj1.dataObj, new EvalAssertionObj())
                // apply defaults if applicable
                if ('applyDataObjDefaults' in patternObj1) {
                    patternObj1.applyDataObjDefaults(patternObj1.dataObj)
                }
                // clear existing result
                resetDataStateObj(patternObj1.resultObj, new EvalResultObj())
            }

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
             }
            }
           if (atLeastOnePropertyWasUpdated) {
              state.isEvalObjUpdated = true
           }
        },
        setFieldSupportValueTypes(state, obj /* object containing an array of fhir enumerated types */) {
           state.fieldSupport.fieldValueTypes = obj
        },
        setFieldSupportOverrides(state, obj) {
            // Use this space to treat object values here if needed
            state.fieldSupport.overrideFieldTypes  = obj
        },
        setFixtureIds(state, obj) {
            state.fixtureIds = obj
        }
    },
    actions: {
    },
}
