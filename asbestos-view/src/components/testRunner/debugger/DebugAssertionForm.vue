<template>
        <div class="flexContainer">
            <div v-for="(propKey, keyIndex) in getPatternTypeObj().displayFieldList" :key="keyIndex">
        <div>
            <div style="text-align: left">
                <label :for="getFormInputId(propKey)" :title="getEnumTypeShortDefinition(propKey)">{{propKey}}</label>
                    <span
                    @click.stop="openHelp(propKey)"
                    class="infoIconLink"
                    :title="`Click to open the TestScript ${propKey} Element Detailed Description in a new browser tab. `">&#x2139;</span> <!-- &#x1f4d6; &#x2139; -->
            </div>
            <div>
                <template v-if="isPropertyAnEnumType(propKey)">
                    <select class="form-control-select"
                            :value="getPropVal(propKey)"
                            :data-prop-key="propKey"
                            @change="onEvalObjPropUpdate">
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
                <span class="smallText">{{getEnumTypeFormalDefinition(propKey)}}</span>
            </div>
        </div>
     </div>
        <div v-bind:class="{
                        'evalNotPassed': getResultCode().valueOf() !== 'pass',
                        }">
        <span class="form-block">{{getResultCode()}}</span>
        <vue-markdown
                v-bind:source="getResultMessage()"></vue-markdown>
    </div>
    </div>
</template>

<script>
    import VueMarkdown from 'vue-markdown'

    export default {
        name: "DebugAssertionForm",
        props: {
            patternTypeId: {
                type: String,
                required: true
            },
        },
        methods: {
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
                let arr = this.$store.state.debugAssertionEval.fieldValueTypes
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
                    return propObj.values
                }
            },
            isPropertyAnEnumType(propKey) {
                let propObj = this.getFieldFromValueType(propKey)
                if (propObj !== null) {
                    return (propObj.values.length > 0)
                }
                return false
            },
            onEvalObjPropUpdate(e) {
                // console.log('onEvalObjProp.. was called.')
                this.$store.commit('setEvalObjProperty', {patternTypeId: this.patternTypeId, propKey: e.target.getAttribute('data-prop-key'), propVal: e.target.value})
            },
            doEval(propKey) {
                this.$store.commit('setDebugAssertionEvalPropKey', {patternTypeId: this.patternTypeId, propKey: propKey})
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
                    this.doEval(event.target.id)
                }, 800);
            },

        },
        components: {
            VueMarkdown,
        }

        }
</script>

<style scoped>
    .flexContainer {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        width: auto;
        height: 50%;
    }

    .infoIconLink {
        vertical-align: top;
        font-size: small;
        cursor: pointer;
        margin-left: 4px;
        margin-bottom: 4px;
        text-align: left;
    }


    .smallText {
        text-align: left;
        font-size: xx-small;
        display: inline-block;
        width: 16em;
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