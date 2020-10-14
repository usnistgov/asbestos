<template>
    <!--
    AssertionRunner does not point to the offending assert element in the TestReport.
    -->
    <div>
        <div class="dafFlexContainer" id="firstRowContainer">
            <div class="dafFlexItem">
                <div>
                    <div>
                        <label :for="getFormInputId('sourceId')" :title="getEnumTypeShortDefinition('sourceId')"
                               class="form-label">sourceId</label>
                        <span
                                :title="`Click to open the sourceId assert element detailed description in a new browser tab.`"
                                @click.stop="openHelp('sourceId')"
                                class="infoIconLink"><img
                                alt="External link" src="../../../assets/ext_link.png"
                                style="vertical-align: top"/></span> <!-- &#x1f4d6; &#x2139; -->
                    </div>
                    <div>
                        <select :data-prop-key="'sourceId'"
                                :id="getFormInputId('sourceId')"
                                :value="getPropVal('sourceId')"
                                @change="onEvalObjPropSelect"
                                class="form-control-select"
                        >
                            <option :disabled="option.codeValue===''"
                                    :key="idx"
                                    :title="option.definition"
                                    :value="option.codeValue"
                                    v-for="(option,idx) in getEnumTypeArray('sourceId')">
                                {{ option.displayName }}
                            </option>
                        </select>
                    </div>
                    <div class="smallText">{{getEnumTypeFormalDefinition('sourceId')}}</div>
                    <div class="sourceIdDetails">
                        <p v-if="getSourceIdDetails.fixtureResourceName">The selected resource is a\an
                            <a :href="getResourceLink(getSourceIdDetails.fixtureProfileUrl)" target="_blank">{{getSourceIdDetails.fixtureResourceName}}<img
                                    alt="External link" src="../../../assets/ext_link.png" style="vertical-align: top"
                                    title="External link"></a>.
                        </p>
                        <div style="width: 700px" v-if="getSourceIdDetails.analysisUrl">
                            <inspect-event
                                    :channelId="$store.state.base.channelId"
                                    :eventId="decodeURIComponent(getSourceIdDetails.analysisUrl)"
                                    :modal-mode="getSourceIdDetails.direction"
                                    :noNav="true"
                                    :sessionId="$store.state.base.session"
                            >
                            </inspect-event>
                        </div>
                    </div>

                </div>
            </div> <!-- End div.dafFlexItem -->


        </div>
        <div class="secondRowFlexContainer">
            <div class="dafFlexItem">
                <div>
                    <div class="inlineDiv">FHIRPath</div>
                    <span
                            :title="`Click to open the FHIRPath reference in a new browser tab.`"
                            @click.stop="openFHIRPathWebsite"
                            class="infoIconLink"><img
                            alt="External link" src="../../../assets/ext_link.png" style="vertical-align: top"/></span>
                    <span>&nbsp;</span>
                    <label :for="getFormInputId('expression')" :title="getEnumTypeShortDefinition('expression')"
                           class="form-label">expression</label>
                    <span
                            :title="`Click to open the expression assert element detailed description in a new browser tab.`"
                            @click.stop="openHelp('expression')"
                            class="infoIconLink"><img
                            alt="External link" src="../../../assets/ext_link.png" style="vertical-align: top"/></span>
                </div>
                <div>
                 <textarea
                         :data-prop-key="'expression'"
                         :id="getFormInputId('expression')"
                         :placeholder="getSourceIdDetails.fixtureResourceName"
                         :value="getPropVal('expression')"
                         @input="onTextChange"
                         cols="40"
                         rows="4"
                         v-bind:class="{
                        'form-control-textarea-error': getResultCode().valueOf() !== 'pass' && getResultPropKey() === 'expression',
                        'form-control-textarea-general' : true,
                        }"
                 />
                    <div class="fhirPathExpressionText">{{getEnumTypeFormalDefinition('expression')}}</div>
                </div>
                <div>
                    <label :for="getFormInputId('operator')" :title="getEnumTypeShortDefinition('operator')"
                           class="form-label">operator</label>
                    <span
                            :title="`Click to open the value element detailed description in a new browser tab.`"
                            @click.stop="openHelp('operator')"
                            class="infoIconLink"><img
                            alt="External link" src="../../../assets/ext_link.png" style="vertical-align: top"/></span>
                </div>
                <div>
                    <template v-if="isPropertyAnEnumType('operator')"> <!--  -->
                        <select class="form-control-select"
                                :id="getFormInputId('operator')"
                                :data-prop-key="'operator'"
                                :value="getPropVal('operator')"
                                @change="onEvalObjPropSelect"
                        >
                            <option v-for="(option,idx) in getEnumTypeArray('operator')"
                                    :value="option.codeValue"
                                    :title="option.definition"
                                    :disabled="option.codeValue===''"
                                    :key="idx">
                                {{ option.displayName }}
                            </option>
                        </select>
                        <div class="smallText">{{getEnumTypeFormalDefinition('operator')}}</div>
                    </template>
                </div>
                <div>
                    <label :for="getFormInputId('value')" :title="getEnumTypeShortDefinition('value')"
                           class="form-label">value</label>
                    <span
                            :title="`Click to open the value element detailed description in a new browser tab.`"
                            @click.stop="openHelp('value')"
                            class="infoIconLink"><img
                            alt="External link" src="../../../assets/ext_link.png" style="vertical-align: top"/></span>
                </div>
                <div>
                <textarea
                        :data-prop-key="'value'"
                        :id="getFormInputId('value')"
                        :value="getPropVal('value')"
                        @input="onTextChange"
                        rows="1"
                        v-bind:class="{
                        'form-control-textarea-error': getResultCode().valueOf() !== 'pass' && getResultPropKey() === 'value',
                        'form-control-textarea-general' : true,
                        }"
                />
                    <div class="smallText">{{getEnumTypeFormalDefinition('value')}}</div>
                </div>

                <button @click="doEval('')" class="evalButton">Evaluate</button>
            </div>
            <div class="dafFlexItemResult">
                <label class="form-label, resultShadow" for="fpeResultsBox"
                       v-if="getPatternTypeObj.resultObj.wasEvaluatedAtleastOnce">result(s):</label>
                <div class="resultBox" id="fpeResultsBox">
                    <div v-if="getPatternTypeObj.resultObj.wasEvaluatedAtleastOnce && getResultCode() === 'pass'">
                        <template
                                v-if="getResourceList() && (getResourceList().length > 0 && !getSourceIdDetails.scalarValueString)">
                            <select size="5">
                                <option :key="rKey"
                                        :value="rName"
                                        v-for="(rName,rKey) in getResourceList()">
                                    {{ rName}}
                                </option>
                            </select>
                            <p>{{getResourceList().length}} resource(s) found.</p>
                        </template>
                        <template v-else-if="getSourceIdDetails.scalarValueString && !getPropVal('value')">
                            <div class="">{{getPropVal('expression')}}
                                <template
                                        v-if="!(getPropVal('expression').endsWith('.value')||getPropVal('expression').includes('='))">
                                    <b>.value</b></template>
                                :
                            </div>
                            <p>{{decodeURIComponent(getSourceIdDetails.scalarValueString)}}</p>
                            <div class="resultShadow">type:</div>
                            <p>{{getResourceList()[0]}}</p>
                        </template>
                        <template v-else>
                            <div class="resultBox">
                                <span class="form-block">{{getResultCode()}}</span>
                                <vue-markdown v-bind:source="getResultMessage()"></vue-markdown>
                            </div>
                        </template>
                    </div>
                    <template v-if="getResultCode().valueOf() !== 'pass'">
                        <div
                                v-bind:class="{
                    'resultBox': true,
                    'evalNotPassed': getResultCode().valueOf() !== 'pass',
                    }"
                                v-if="getPropVal('expression')">
                            <span class="form-block">{{getResultCode()}}</span>
                            <vue-markdown
                                    v-bind:source="getResultMessage()"></vue-markdown>
                        </div>
                    </template>
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
    .horizFlexItem {
    }

    .secondRowFlexContainer {
        display: flex;
        flex-direction: row;
        flex-wrap: nowrap;
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
        flex-basis: min-content;

    }

    .dafFlexItemResult {
        /*width: 16em;*/
        /*flex: 1; * shorthand. to be expanded by css. */
        /*height: 100px;*/
        margin-left: 40px;
        margin-top: 4px;
        flex-basis: auto;
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
        /*margin-top: 9px;*/
    }

    .sourceIdDetails {
        margin-top: 9px;
        margin-bottom: 9px;
        margin-left: 5px;
    }

    .fhirPathExpressionText,
    .smallText {
        text-align: left;
        font-size: xx-small;
        display: block;
        width: 24em;
        color: gray;
        padding: 0.5em 1em;
    }

    .fhirPathExpressionText {
        width: 34em;
    }

    .text-right {
        text-align: right;
    }

    .form-label {
        /*margin: 4px;*/
    }

    .resultShadow {
        text-decoration: underline;
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
    .form-control-textarea {
        display: block;
        vertical-align: top;
        /*margin-left: 5px;*/
        margin-right: 5px;
        resize: both;
        border-radius: 6px;
    }

    .form-control-general {
        height: 2em;
        width: 16em;
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