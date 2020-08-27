<template>
   <div class="dafFlexContainer">
     <div class="dafFlexItem" v-for="(propKey, keyIndex) in displayFieldList" :key="keyIndex">
        <div>
            <div >
                <label :for="getFormInputId(propKey)" :title="getEnumTypeShortDefinition(propKey)">{{propKey}}</label>
                    <span
                    @click.stop="openHelp(propKey)"
                    class="infoIconLink"
                    :title="`Click to open the ${propKey} assert element detailed description in a new browser tab.`">&#x2139;</span> <!-- &#x1f4d6; &#x2139; -->
            </div>
            <div>
                <template v-if="isPropertyAnEnumType(propKey)">
                    <select class="form-control-select"
                            :id="getFormInputId(propKey)"
                            :value="getPropVal(propKey)"
                            :data-prop-key="propKey"
                            @change="onEvalObjPropSelect">
                        <option v-for="option in getEnumTypeArray(propKey)"
                                :value="option.codeValue" :title="option.definition"
                                :key="option.codeValue">
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
     <div v-bind:class="{
                    'evalNotPassed': getResultCode().valueOf() !== 'pass',
                    }">
        <span class="form-block">{{getResultCode()}}</span>
        <vue-markdown
            v-bind:source="getResultMessage()"></vue-markdown>
     </div>
       <template v-if="resizeContents()"><!-- form_resized --></template>
 </div>
</template>

<script>
    import VueMarkdown from 'vue-markdown'

    export default {
        name: "DebugAssertionEvalForm",
        data() {
            return {
                isResized: false,
            }
        },
        props: {
            patternTypeId: {
                type: String,
                required: true
            },
            // isShown: {
            //     type: Boolean,
            //     required: true
            // }
        },
        created: function() {
            // Reset the resize flag
            this.isResized = false
           // this.resizeContents()
        },
        computed: {
            displayFieldList() {
                return this.getPatternTypeObj().displayFieldList
            }
        },
        watch: {
           // 'isShown': 'resizeContents'
        },
        methods: {
            /*
            Works best when the pattern type with the most objects, such as the All Parameters, is displayed first.
             */
            resizeContents() {
                if (this.isResized)
                    return false
                let containerEl = document.querySelector('div.dafFlexContainer')
                if (containerEl === null)
                    return
                try {
                    let flexItemWidth = 230
                    let flexItemHeight = 114
                    const elementCount = this.displayFieldList.length
                    // for (const fieldName of this.displayFieldList) {
                        let elList = document.querySelectorAll('div.dafFlexItem')
                        // console.log(`elList length: ${elList.length}`)
                        if (elList !== null) {
                            for (let el of elList) {
                                // console.log(`clientWidth is ${el.classList.contains('dafFlexItem')}, clientHeight is ${el.classList}`)
                                if (flexItemWidth < el.clientWidth) {
                                    flexItemWidth = el.clientWidth
                                }
                                if (flexItemHeight < el.clientHeight) {
                                    flexItemHeight = el.clientHeight
                                }
                            }
                        } /* else {
                            // console.log("Element was not found. Using default for resize calculations.")
                        } */
                    if (flexItemWidth > 300)
                        flexItemWidth = 230
                    if (flexItemHeight > 400)
                        flexItemHeight = 114
                    // }
                    const maxFieldsPerColumn = 6
                    const fieldsPerColumn = (elementCount > maxFieldsPerColumn)? maxFieldsPerColumn : elementCount
                    let minFormHeight = flexItemHeight * fieldsPerColumn // Default to 6 fields per column
                    // Enable the singleColumnHeight if form should by dynamically sized if fewer than 6 columns may be present
                    // const singleColumnHeight = (flexItemHeight * elementCount)
                    // if (singleColumnHeight < 500) {
                    //     minFormHeight = singleColumnHeight
                    // }
                    const estimateWidth = Math.round((elementCount / fieldsPerColumn) * flexItemWidth) // Math.round((elementCount / (minFormHeight / flexItemHeight)) * flexItemWidth)
                    // console.log(`length: ${elementCount}, flexItemWidth: ${flexItemWidth}, flexItemHeight: ${flexItemHeight}, minFormHeight: ${minFormHeight}, estimateWidth: ${estimateWidth}`)
                    containerEl = document.querySelector('div.eval-modal-container') //('div.dafFlexContainer') // div.eval-modal-container
                    if (containerEl !== null) {
                        /* outerShell... to compensate for the pattern type menu buttons */
                        let outerShellHeight = 200
                        let outerShellWidth = 53
                        let buttonsContainerEl = document.querySelector('div.patternHeaderButtons')
                        if (buttonsContainerEl !== null) {
                            outerShellHeight = 163 /* Eval header banner and Select a pattern message */ + buttonsContainerEl.clientHeight
                            // console.log(' ' + outerShellHeight)
                        }
                        // console.log(`setting el height to  ${minFormHeight}`)
                        const finalOuterContainerHeight = (outerShellHeight+minFormHeight)
                        containerEl.style.height = finalOuterContainerHeight + 'px'
                        containerEl.style.width = (outerShellWidth+estimateWidth) + 'px'
                        // tracer
                        // console.log('outerContainerFinalHeight: ' + finalOuterContainerHeight)
                        // containerEl.style.border = '1px solid red'
                    }
                    containerEl = document.querySelector('div.dafFlexContainer') //('div.dafFlexContainer') // div.eval-modal-container
                    if (containerEl !== null) {
                        this.isResized = true // This element is not rendered at the time resize is run
                        // console.log(`setting el height to  ${minFormHeight}`)
                        containerEl.style.height = (minFormHeight) + 'px'
                        containerEl.style.width = (estimateWidth) + 'px'
                        // tracer
                        // containerEl.style.border = '1px dashed black'
                    }
                } catch(err) {
                    console.log('error: ' + err)
                }
                console.log('Form was resized.')
                return this.isResized
            },
            openHelp(propKey) {
                window.open("http://hl7.org/fhir/testscript-definitions.html#TestScript.setup.action.assert." + propKey, "_blank")
            },
            getFormInputId(propKey) {
               return this.patternTypeId + '_' + propKey;
            },
            getPatternTypeObj() {
                let obj = this.$store.state.debugAssertionEval.evalObjByPattern.patternTypes[this.patternTypeId]
                // console.log('got ' + this.patternTypeId)
                // console.log('field list length: ' + obj.displayFieldList.length)
                return obj
            },
            getResultCode() {
                return this.getPatternTypeObj().resultObj.resultMessage
            },
            getResultMessage() {
                return this.getPatternTypeObj().resultObj.markdownMessage
            },
            getResultPropKey() {
                return this.getPatternTypeObj().resultObj.propKey
            },
            getPropVal(propKey) {
                return this.getPatternTypeObj().dataObj[propKey]
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
                if (propObj !== null) {
                    if (propObj.values.length > 0) {
                        return propObj.values
                    } else {
                        let overrideObj = this.getOverrideType(propKey)
                        if (overrideObj !== null && overrideObj.values.length > 0) {
                            return overrideObj.values
                        }
                    }
                }
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
                let assertDataString = JSON.stringify(this.getPatternTypeObj().dataObj)
                // console.log('before base64: ' + assertDataString)
                this.$store.dispatch('doDebugEvalAssertion', window.btoa(assertDataString))
            },
            evalOnKeyUp: function (event) {
                if (this.evalTimer) {
                    clearTimeout(this.evalTimer);
                    this.evalTimer = null;
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
        /*height: 400px; !* 400px; *!*/
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

</style>