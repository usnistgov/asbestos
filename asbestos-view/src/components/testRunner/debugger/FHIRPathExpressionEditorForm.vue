<template>
    <!--
    AssertionRunner does not point to the offending assert element in the TestReport.
    -->
  <div>
   <div id="firstRowContainer" class="dafFlexContainer">
     <div class="dafFlexItem" > <!-- v-for="(propKey, keyIndex) in displayFieldList" :key="keyIndex" :data-flex-item="propKey" -->
        <div>
            <div>
                <label :for="getFormInputId('sourceId')" :title="getEnumTypeShortDefinition('sourceId')">sourceId</label>
                    <span
                    @click.stop="openHelp('sourceId')"
                    class="infoIconLink"
                    :title="`Click to open the sourceId assert element detailed description in a new browser tab.`"><img alt="External link" style="vertical-align: top" src="../../../assets/ext_link.png"/></span> <!-- &#x1f4d6; &#x2139; -->
            </div>
            <div>
                    <select class="form-control-select"
                            :id="getFormInputId('sourceId')"
                            :data-prop-key="'sourceId'"
                            :value="getPropVal('sourceId')"
                            @change="onEvalObjPropSelect"
                            >
                        <option v-for="(option,idx) in getEnumTypeArray('sourceId')"
                                :value="option.codeValue"
                                :title="option.definition"
                                :disabled="option.codeValue===''"
                                :key="idx">
                            {{ option.displayName }}
                        </option>
                    </select>
            </div>
            <div class="smallText">{{getEnumTypeFormalDefinition(propKey)}}</div>
            <div class="sourceIdDetails" v-if="isFhirPathOptionTab && propKey === getSourceIdDetails.displayField">
                <p v-if="getSourceIdDetails.fixtureResourceName">The selected resource is a\an
                    <a :href="getResourceLink(getSourceIdDetails.fixtureProfileUrl)" target="_blank">{{getSourceIdDetails.fixtureResourceName}}<img alt="External link" style="vertical-align: top" title="External link" src="../../../assets/ext_link.png"></a>.
                </p>
                <div v-if="getSourceIdDetails.analysisUrl" style="width: 700px">
                    <inspect-event
                            :sessionId="$store.state.base.session"
                            :channelId="$store.state.base.channelId"
                            :eventId="decodeURIComponent(getSourceIdDetails.analysisUrl)"
                            :noNav="true"
                    >
                    </inspect-event>
                </div>
            </div>





        </div>
     </div> <!-- End div.dafFlexItem -->


  </div>
    <div class="secondRowFlexContainer" >
       <div class="horizFlexItem">
              <textarea
                      v-bind:class="{
                        'form-control-textarea-error': getResultCode().valueOf() !== 'pass' && getResultPropKey() === 'expression',
                        'form-control-textarea' : true,
                        }"
                      :id="getFormInputId('expression')"
                      :value="getPropVal('expression')"
                      :data-prop-key="expression"
                      @input="onTextChange"
              />
           <div class="smallText">{{getEnumTypeFormalDefinition('expression')}}</div>
           <textarea
                   v-bind:class="{
                        'form-control-textarea-error': getResultCode().valueOf() !== 'pass' && getResultPropKey() === 'value',
                        'form-control-textarea' : true,
                        }"
                   :id="getFormInputId('value')"
                   :value="getPropVal('value')"
                   :data-prop-key="value"
                   @input="onTextChange"
           />
           <div class="smallText">{{getEnumTypeFormalDefinition('value')}}</div>

           <button class="evalButton" @click="doEval('')">Evaluate</button>
       </div>
        <div class="horizFlexItem">
            <div class="resultBox" v-if="isFhirPathOptionTab">
                <div v-if="getPatternTypeObj.resultObj.wasEvaluatedAtleastOnce && getResultCode() === 'pass'" >
                    <template v-if="getResourceList() && getResourceList().length > 0">
                        <select size="5"  >
                            <option v-for="(rName,rKey) in getResourceList()"
                                    :value="rName"
                                    :key="rKey">
                                {{ rName}}
                            </option>
                        </select>
                        <div class="form-label">{{getResourceList().length}} resource(s) found.</div>
                        <!--                   <div>-->
                        <!--                       <button class="resultBox">Inspect</button>-->
                        <!--                   </div>-->
                    </template>
                    <template v-else>
                        No resources found.
                    </template>
                </div>
                <div v-if="getPropVal('expression')"
                     v-bind:class="{
                    'resultBox': true,
                    'evalNotPassed': getResultCode().valueOf() !== 'pass',
                    }">
                    <span class="form-block">{{getResultCode()}}</span>
                    <vue-markdown
                            v-bind:source="getResultMessage()"></vue-markdown>
                </div>
            </div>
        </div>
    </div>
  </div>
</template>

<script>
    import VueMarkdown from 'vue-markdown'
    import InspectEvent from "../../logViewer/InspectEvent";
    import debugEvalFormMixin from "../../../mixins/debugEvalFormMixin";

    export default {
        name: "FHIRPathExpressionEditorForm",
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
            InspectEvent
        },
        mixins: [
            debugEvalFormMixin,
        ],

    }
</script>

<style scoped>
    .secondRowFlexContainer {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        width: auto; /* auto; */
        height: auto; /* A definite pixel limit is needed for the flex mode column wrap */
        text-align: left;
    }
    .dafFlexContainer {
        display: flex;
        flex-direction: column;
        flex-wrap: wrap;
        width: auto; /* auto; */
        height: auto; /* A definite pixel limit is needed for the flex mode column wrap */
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