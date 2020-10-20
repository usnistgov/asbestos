export default {
    data() {
        return {
        }
    },
    computed: {
        getPatternTypeObj() {
            return this.patternTypeObj
            // let obj = this.$store.state.debugAssertionEval.evalObjByPattern.patternTypes[this.patternTypeId]
            // if (this.optionType === 'fhirPathTab') {
            //    obj = obj.fhirPathContextObj
            // }
            // console.log('got ' + this.patternTypeId)
            // console.log('field list length: ' + obj.displayFieldList.length)
            // return obj
        },
        displayFieldList() {
            return this.getPatternTypeObj.displayFieldList
        },
        defaultDisplayFieldList() {
            return this.getDefaultPatternTypeObj().displayFieldList
        },
        isFhirPathOptionTab() {
            return this.optionType === 'fhirPathTab'
        },
        getSourceIdDetails() {
            if (this.isFhirPathOptionTab) {
                return this.getPatternTypeObj.sourceIdDetails
            }
            return ''
        }
    },
    methods: {
        getResourceLink(encodedUri) {
            return decodeURIComponent(encodedUri)
        },
        openHelp(propKey) {
            window.open("http://hl7.org/fhir/testscript-definitions.html#TestScript.setup.action.assert." + propKey, "_blank")
        },
        openFHIRPathWebsite() {
            window.open("http://hl7.org/fhirpath/N1/index.html", "_blank");
        },
        getFormInputId(propKey) {
            return this.patternTypeId + '_' + this.optionType + '_' + propKey;
        },
        getDefaultPatternTypeObj() {
            const defaultPatternTypeId = this.$store.state.debugAssertionEval.defaultPatternTypeId
            let obj = this.$store.state.debugAssertionEval.evalObjByPattern.patternTypes[defaultPatternTypeId]
            return obj
        },
        getResultCode() {
            return this.getPatternTypeObj.resultObj.resultMessage
        },
        getResultMessage() {
            return this.getPatternTypeObj.resultObj.markdownMessage
        },
        getResourceList() {
            const obj = this.getPatternTypeObj
            if ('resourceList' in obj) {
                return obj.resourceList
            }
            return null
        },
        getResultPropKey() {
            if (this.getPatternTypeObj && 'propKey' in this.getPatternTypeObj.resultObj)
                return this.getPatternTypeObj.resultObj.propKey
            else
                return ''
        },
        getPropVal(propKey) {
            let val = this.getPatternTypeObj.dataObj[propKey]
            // console.log(`patternType is ${this.patternTypeId}, optionType is ${this.optionType}, reading key ${propKey}, and the value is '${val}'.`)
            // console.log(`value is empty? ${val===''}`)
            return val
        },
        getFieldFromValueType(propKey) {
            let arr = this.$store.state.debugAssertionEval.fieldSupport.fieldValueTypes
            let result = this.searchField(arr, propKey)
            return result
        },
        searchField(arr, propKey) {
            if (arr !== null) {
                let results = arr.filter(item => item.name === propKey) // Find the first matching item
                if (results.length === 1) { // Should be the only one in the result
                    return results[0] // return the obj
                }
            }
            return null

        },
        getEnumTypeShortDefinition(propKey) {
            let propObj = this.getFieldFromValueType(propKey)
            if (propObj !== null) {
                return propObj.shortDefinition
            }
            return ''
        },
        getEnumTypeFormalDefinition(propKey) {
            let propObj = this.getFieldFromValueType(propKey)
            if (propObj !== null) {
                return propObj.formalDefinition
            }
            return ''
        },
        getEnumTypeArray(propKey) {
            let propObj = this.getFieldFromValueType(propKey)
            let valuesObj = null
            if (propObj !== null) {
                if (propObj.values.length > 0) {
                    valuesObj = propObj.values
                } else {
                    let overrideObj = this.getOverrideType(propKey)
                    if (overrideObj !== null && overrideObj.values.length > 0) {
                        valuesObj = overrideObj.values
                    }
                }
            }
            if (valuesObj != null && valuesObj.length > 0) {
                let disabledOptionArr = [{displayName: 'Please select one', codeValue: '', definition:''}]
                return disabledOptionArr.concat(valuesObj) // Need to return a new array without disturbing the static copy
            }
            return valuesObj

        },
        getOverrideType(propKey) {
            let result = null
            // Add a condition to exclude pattern types to automatically override field some field types
            // if (this.patternTypeId !== this.$store.state.debugAssertionEval.defaultPatternTypeId) {
            // Map fixtureId to sourceId
            if (propKey === 'sourceId' || propKey === 'compareToSourceId' || propKey === 'minimumId') {
                result = this.$store.state.debugAssertionEval.fixtureIds
            } else {
                let arr = this.$store.state.debugAssertionEval.fieldSupport.overrideFieldTypes
                result = this.searchField(arr, propKey)
            }
            // }
            return result
        },
        isPropertyAnEnumType(propKey) {
            let result = this.getFieldFromValueType(propKey)
            if (result !== null) {
                let noValues = (result.values.length === 0)
                if (noValues) {
                    // Allow override for non-default pattern type form. Maybe it is useful to have free-text entry in the default form type.
                    result = this.getOverrideType(propKey)
                }
                if (result !== null)
                    return result.values.length > 0
            }
            return false
        },
        onEvalObjPropSelect(e) {
            this.onEvalObjPropUpdate(e)
            let propKey = e.target.getAttribute('data-prop-key')
            this.doEval(propKey)
        },
        onTextChange(e) {
            this.onEvalObjPropUpdate(e)
            const propKey = e.target.getAttribute('data-prop-key')
            if (this.evalTimer) {
                clearTimeout(this.evalTimer)
                this.evalTimer = null
            }
            this.evalTimer = setTimeout(() => {
                this.doEval(propKey)
            }, 800);
        },
        onEvalObjPropUpdate(e) {
            const propKey = e.target.getAttribute('data-prop-key')
            this.$store.commit('setEvalObjProperty', {patternTypeId: this.patternTypeId, propKey: propKey, propVal: e.target.value})
        },
        doEval(propKey) {
            this.$store.commit('setDebugAssertionEvalPropKey', {patternTypeId: this.patternTypeId, propKey: propKey}) // Just to track what changed field was updated in the last attempt so the error hint may be applied to this field
            let assertDataString = JSON.stringify(this.getPatternTypeObj.dataObj)
            // console.log('before base64: ' + assertDataString)
            const evalContextObj = this.getPatternTypeObj
            let evalAction = this.$store.state.debugAssertionEval.evalObjByPattern.defaultEvalAction
            if ('evalAction' in evalContextObj) {
                evalAction = evalContextObj.evalAction
            }
            this.$store.dispatch(evalAction, window.btoa(assertDataString))
        },
    },
}