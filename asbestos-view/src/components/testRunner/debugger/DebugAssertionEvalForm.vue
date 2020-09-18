<template>
    <!--
    Notes on the Debug assertion Eval window (and running an assertion in general):
    Each assertion property is evaluated in an arbitrary order. The implication of this is that when Eval is executed for the entire assert object,
    an error could have occurred in other place than the most recent place of change.
    Example: prop1=true, prop2='OperationOutcome.count() = 0', Now if prop1 was actually false, the assertion runner will stop at the first failure.
    AssertionRunner does not point the offending assert element in the TestReport.
    -->
   <div class="dafFlexContainer">
     <div class="dafFlexItem" v-for="(propKey, keyIndex) in displayFieldList" :key="keyIndex" :data-flex-item="propKey">
        <div>
            <div >
                <label :for="getFormInputId(propKey)" :title="getEnumTypeShortDefinition(propKey)">{{propKey}}</label>
                    <span
                    @click.stop="openHelp(propKey)"
                    class="infoIconLink"
                    :title="`Click to open the ${propKey} assert element detailed description in a new browser tab.`">&#x2139;</span> <!-- &#x1f4d6; &#x2139; -->
            </div>
            <div>
                <template v-if="isPropertyAnEnumType(propKey)"> <!--  -->
                    <select class="form-control-select"
                            :id="getFormInputId(propKey)"
                            :data-prop-key="propKey"
                            :value="getPropVal(propKey)"
                            @change="onEvalObjPropSelect"
                            >
                        <option v-for="(option,idx) in getEnumTypeArray(propKey)"
                                :value="option.codeValue"
                                :title="option.definition"
                                :disabled="option.codeValue===''"
                                :key="idx">
                            {{ option.displayName }}
                        </option>
                    </select>
                </template>
                <template v-else>
                     <textarea
                             v-bind:class="{
                        'form-control-textarea-error': getResultCode().valueOf() !== 'pass' && getResultPropKey() === propKey,
                        'form-control-textarea' : true,
                        }"
                             :id="getFormInputId(propKey)"
                             :value="getPropVal(propKey)"
                             :data-prop-key="propKey"
                             @input="onEvalObjPropUpdate"
                             @keyup="evalOnKeyUp"
                     />
                </template>
            </div>
            <div class="smallText">{{getEnumTypeFormalDefinition(propKey)}}</div>
        </div>
     </div>
       <button class="evalButton" @click="doEval('')">Evaluate</button>
     <div v-bind:class="{
                    'resultBox': true,
                    'evalNotPassed': getResultCode().valueOf() !== 'pass',
                    }">
        <span class="form-block">{{getResultCode()}}</span>
        <vue-markdown
            v-bind:source="getResultMessage()"></vue-markdown>
         <div v-if="getPatternTypeObj.resultObj.wasEvaluatedAtleastOnce && getResultCode() === 'pass'" >
             <template v-if="getResourceList() && getResourceList().length > 0">
                <select size="5"  >
                 <option v-for="(rName,rKey) in getResourceList()"
                         :value="rName"
                         :key="rKey">
                     {{ rName}}
                 </option>
              </select>
              <div class="form-label">{{getResourceList().length}} resource(s) were returned.</div>
             <div>
                <button class="resultBox">Inspect</button>
             </div>
            </template>
             <template v-else>
                 No resources found.
             </template>
         </div>
     </div>
 </div>
</template>

<script>
    import VueMarkdown from 'vue-markdown'

    export default {
        name: "DebugAssertionEvalForm",
        data() {
            return {
                evalTimer: null,
            }
        },
        props: {
            patternTypeObj: {
               type: Object,
               required: true
            },
            patternTypeId: {
                type: String,
                required: true
            },
            optionType: {
                type: String,
                required: true
            }
            // isShown: {
            //     type: Boolean,
            //     required: true
            // }
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
        },
        methods: {
            openHelp(propKey) {
                window.open("http://hl7.org/fhir/testscript-definitions.html#TestScript.setup.action.assert." + propKey, "_blank")
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
                return this.getPatternTypeObj.resultObj.propKey
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
            onEvalObjPropUpdate(e) {
                // console.log('onEvalObjProp.. was called.')
                this.$store.commit('setEvalObjProperty', {patternTypeId: this.patternTypeId, propKey: e.target.getAttribute('data-prop-key'), propVal: e.target.value})
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
            evalOnKeyUp: function (event) {
                if (this.evalTimer) {
                    clearTimeout(this.evalTimer)
                    this.evalTimer = null
                }
                this.evalTimer = setTimeout(() => {
                    this.doEval(event.target.getAttribute('data-prop-key'))
                }, 800);
            },

        },
        components: {
            VueMarkdown,
        }

        }
</script>

<style scoped>
    .dafFlexContainer {
        display: flex;
        flex-direction: column;
        flex-wrap: wrap;
        width: auto; /* auto; */
        height: 754px; /* A definite pixel limit is needed for the flex mode column wrap */
        text-align: left;
    }

    .dafFlexItem {
        /*width: 16em;*/
        /*flex: 1; * shorthand. to be expanded by css. */
        /*height: 100px;*/
        margin-top: 4px;
        flex-basis: min-content ;

    }

    .infoIconLink {
        vertical-align: top;
        font-size: small;
        cursor: pointer;
        margin-left: 4px;
        margin-bottom: 4px;
        text-align: left;
    }

    .evalNotPassed {
        color: red;
    }

    .resultBox {
        margin-top: 5px;
    }

    .smallText {
        text-align: left;
        font-size: xx-small;
        display: inline-block;
        width: 24em;
        color: gray;
        padding: 0.5em 1em;

    }
    .text-right {
        text-align: right;
    }

    .form-label {
        margin-bottom: 4px;
    }

    .form-block {
        display: block;
    }

    .form-label > .form-control {
        margin-top: 0.5em;
    }

    .form-control {
        /*display: block;*/
        width: 70%;
        padding: 0.5em 1em;
        line-height: 1.5;
        border: 1px solid #ddd;
        margin-bottom: 1em;
    }

    .form-control-select,
    .form-control-textarea-error,
    .form-control-textarea {
        vertical-align: top;
        margin-left: 5px;
        margin-right: 5px;
        height: 2em;
        width: 16em;
        resize: both;
        border-radius: 6px;
    }

    .form-control-select {
        resize: none;
    }

    .form-control-textarea-error {
        border: 2px solid red;
    }

    .evalButton {
        margin: 10px;
        border-radius: 3px;
        background-color: lavender; /* #FFC83D; */
        font-size: x-small;
        width: 20em;
    }
</style>