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
                <label class="form-label" :for="getFormInputId(propKey)" :title="getEnumTypeShortDefinition(propKey)">{{propKey}}</label>
                    <span
                    @click.stop="openHelp(propKey)"
                    class="infoIconLink"
                    :title="`Click to open the ${propKey} assert element detailed description in a new browser tab.`"><img alt="External link" style="vertical-align: top" src="../../../assets/ext_link.png"></span> <!-- &#x1f4d6; &#x2139; -->
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
                             @input="onTextChange"
                     />
                </template>
            </div>
            <div class="smallText">{{getEnumTypeFormalDefinition(propKey)}}</div>
        </div>
     </div> <!-- end flexItem -->
     <button class="evalButton" @click="doEval('')">Evaluate</button>
     <div
             v-bind:class="{
                    'resultBox': true,
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
    import debugEvalFormMixin from "../../../mixins/debugEvalFormMixin";

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
        components: {
            VueMarkdown,
        },
        mixins: [
            debugEvalFormMixin,
        ],

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
        /*margin-left: 4px;*/
        margin-bottom: 4px;
        text-align: left;
    }

    .evalNotPassed {
        color: red;
    }

    .resultBox {
        margin-top: 9px;
    }

    .sourceIdDetails {
        margin-top: 9px;
        margin-bottom: 9px;
        margin-left: 5px;
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
        margin: 4px;
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