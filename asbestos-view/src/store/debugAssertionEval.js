import Vue from "vue";
import Vuex from "vuex";

Vue.use(Vuex)

function XpathNotSupportedFooter() {
    this.message = 'Note: FHIR Toolkit does not support XPath/JSONPath.'
}

function AssertionEvalObj() {
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

function FhirPathContextObj() {
    this.dataObj = new AssertionEvalObj()
    this.resultObj = new EvalResultObj()
    this.resourceList = []
    this.displayFieldList = ['sourceId','expression','value']
    this.footerList = [new XpathNotSupportedFooter()]
    this.evalAction = 'doDebugEvalForResources'
}

export const debugAssertionEvalStore = {
    state() {
        return {
            assertionEvalBreakpointIndex: '',
            showEvalModalDialog: false,
            isEvalObjUpdated: false,
            selectedPatternTypeId: 'AllParameters', // Initial string contains default pattern
            defaultPatternTypeId: 'AllParameters',
            defaultEvalOptionTab: 'assertionTab',
            tabMap: {assertionTab: {label: 'Assertion'}
                    ,fhirPathTab: {label: 'FHIRPath Expression Editor'}},
            evalObjByPattern: {
                    defaultEvalAction: 'doDebugEvalAssertion',
                    patternTypes:
                        {
                            AllParameters: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj(), displayFieldList: ['label','description','direction'
                                    ,'compareToSourceId','compareToSourceExpression','compareToSourcePath','contentType','expression','headerField','minimumId',
                                    'navigationLinks','operator','path','requestMethod','requestURL','resource','response','responseCode','sourceId','validateProfileId','value','warningOnly']
                                    , fhirPathContextObj: new FhirPathContextObj()
                                    , selectedEvalOptionTab: ''
                                    , footerList: [new XpathNotSupportedFooter()]
                                    },
                          /*
                            Asbestos Test Engine does not support the compareToSourcePath element part of the CompareToSourceId pattern type.
                           */
                            CompareToSourceId: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['compareToSourceId','compareToSourceExpression','warningOnly']
                                , fhirPathContextObj: new FhirPathContextObj()
                                , selectedEvalOptionTab: ''
                                , footerList: [new XpathNotSupportedFooter()] },
                            ContentType: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['contentType','warningOnly']},
                            Expression: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['sourceId','expression','warningOnly']
                                , fhirPathContextObj: new FhirPathContextObj()
                                , selectedEvalOptionTab: ''
                                , footerList: [new XpathNotSupportedFooter()]},
                            ExpressionValue: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['sourceId','expression','value','warningOnly']
                                , fhirPathContextObj: new FhirPathContextObj()
                                , selectedEvalOptionTab: ''
                                , footerList: [new XpathNotSupportedFooter()]},
                            HeaderField: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['headerField','warningOnly']},
                            MinimumId: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['minimumId','warningOnly']},
                            RequestMethod: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['sourceId','requestMethod','warningOnly']
                                , fhirPathContextObj: new FhirPathContextObj()
                                , selectedEvalOptionTab: ''
                                , footerList: [new XpathNotSupportedFooter()]
                                , applyDataObjDefaults: function(dataObj) { // To be called by resetDataState
                                    dataObj.requestMethod = 'get' // default to HTTP GET
                                }
                            },
                            ResourceType: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['resource','warningOnly']},
                            Response: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj()
                                , displayFieldList: ['sourceId','response','warningOnly']
                                , fhirPathContextObj: new FhirPathContextObj()
                                , selectedEvalOptionTab: ''
                            },
                            ResponseCode: {dataObj: new AssertionEvalObj(), resultObj: new EvalResultObj()
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
                let patternObj = state.evalObjByPattern.patternTypes[obj.patternTypeId]
                let dataRef = patternObj.dataObj
                if ('selectedEvalOptionTab' in patternObj) {
                    if (patternObj.selectedEvalOptionTab === 'fhirPathTab') {
                        dataRef = patternObj.fhirPathContextObj.dataObj
                    }
                }
                if (obj.propKey in dataRef) {
                    dataRef[obj.propKey] = obj.propVal
                }
            }
        },
        setShowDebugEvalModal(state, bVal) {
            state.showEvalModalDialog = Boolean(bVal)
        },
        setDebugAssertionEvalPropKey(state, obj) {
            let patternObj = state.evalObjByPattern.patternTypes[obj.patternTypeId]
            let resultRef = patternObj.resultObj
            if ('selectedEvalOptionTab' in patternObj) {
                if (patternObj.selectedEvalOptionTab === 'fhirPathTab') {
                    resultRef = patternObj.fhirPathContextObj.resultObj
                }
            }
            resultRef.propKey = obj.propKey
        },
        setSelectedPatternTypeId(state, patternTypeId) {
           state.selectedPatternTypeId = patternTypeId
        },
        setSelectedEvalOptionTab(state, tabId) {
            let patternTypeObj = state.evalObjByPattern.patternTypes[state.selectedPatternTypeId]
            if ('selectedEvalOptionTab' in patternTypeObj) {
               patternTypeObj.selectedEvalOptionTab = tabId
            }
        },
        setDebugAssertionEvalResult(state, obj) {
            try {
                let patternObj = state.evalObjByPattern.patternTypes[state.selectedPatternTypeId]
                if ('selectedEvalOptionTab' in patternObj) {
                   if (patternObj.selectedEvalOptionTab === 'fhirPathTab') {
                      patternObj = patternObj.fhirPathContextObj
                   }
                }
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
            } catch (e) {console.log(e.toString())}

        },
        setEvalForResourcesResult(state, obj) {
            try {
                let contextObj = state.evalObjByPattern.patternTypes[state.selectedPatternTypeId].fhirPathContextObj

                // Remove all array elements
                if (contextObj.resourceList.length > 0) {
                    contextObj.resourceList.splice(0, contextObj.resourceList.length)
                }

                // Reload elements from the result object into the pattern type
                if ('resourceList' in obj && 'resourceList' in contextObj) {
                    for (let r of obj.resourceList) {
                        contextObj.resourceList.push(r)
                    }
                }
            } catch (e) {console.log(e.toString())}
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
                resetDataStateObj(patternObj1.dataObj, new AssertionEvalObj())
                // apply defaults if applicable
                if ('applyDataObjDefaults' in patternObj1) {
                    patternObj1.applyDataObjDefaults(patternObj1.dataObj)
                }
                // clear existing result
                resetDataStateObj(patternObj1.resultObj, new EvalResultObj())

                if ('fhirPathContextObj' in patternObj1) {
                    let fhirPathObj = patternObj1.fhirPathContextObj
                   resetDataStateObj(fhirPathObj.dataObj, new AssertionEvalObj())
                   resetDataStateObj(fhirPathObj.resultObj, new EvalResultObj())
                }

                if ('selectedEvalOptionTab' in patternObj1) {
                    patternObj1.selectedEvalOptionTab = state.defaultEvalOptionTab
                }
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
