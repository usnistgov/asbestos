<template>
    <div name="modal" @dragover="drag_over" @drop="drop" >
        <div class="modal-mask" @click.stop="close" v-show="show" >
            <div class="modalFlexContainer" >
                <div id="debugAssertionEvalModal" class="eval-modal-container" @click.stop>
                    <div>
                        <div id="daemHeader">
                            <span class="eval-modal-header" draggable="true" @dragstart="drag_start">
                                <h3>Eval
<!--                                <span class="closeXIcon" @click.stop="f()">&#x1F504;</span>-->
                                    <span class="closeXIcon" title="Close" @click.stop="close()">&#x274c;</span></h3>
                            </span>
                            <div class="patternHeaderContainer">
                                <div class="patternHeader">Select an Assertion Pattern:</div>
                                <div class="patternHeaderButtons">
                                    <div v-for="(val, patternType) in $store.state.debugAssertionEval.evalObjByPattern.patternTypes"
                                         :key="patternType">
                                        <div
                                                v-bind:class="{
                                            'selectedPatternType': isSelectedPatternType(patternType),
                                            'selectPatternType' : ! isSelectedPatternType(patternType),
                                            }"
                                                @click="doSelectPatternTypeId(patternType)">{{patternType}}</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div id="daemBody" class="modal-body">
                            <template v-if="hasFhirPathExpression">
                                <div>
                                    <div class="evalOptionTabHeader">
                                        <!-- tabKey is the tabObject name -->
                                        <div v-for="(tabObj, tabKey) in tabMap" :key="tabKey"
                                             :class="{'selectedEvalOptionTab':isSelectedOptionTab(tabKey),'evalOptionTab':!isSelectedOptionTab(tabKey)}"
                                              @click="doSelectEvalOptionTab(tabKey)">
                                            {{tabObj.label}}
                                        </div>
                                    </div>
                                </div>
                            </template>
                            <div v-if="isFhirPathTabSelected">
                                <f-h-i-r-path-expression-editor-form :pattern-type-obj="getSelectedObj" :pattern-type-id="selectedPatternTypeId" :option-type="selectedEvalOptionTab" />
                            </div>
                            <div v-else>
                                <debug-assertion-eval-form :pattern-type-obj="getSelectedObj" :pattern-type-id="selectedPatternTypeId" :option-type="selectedEvalOptionTab" />
                            </div>
                        </div>
                        <div id="daemFooter" >
                            <div class="modal-footer text-right">
                                <p v-if="! wasEvaluatedAtLeastOnce">Contents have not yet been evaluated. Evaluation is automatically triggered when an input value is changed by the user or when the Evaluate button is clicked.</p>
                                <template v-if="footerList">
                                    <p v-for="(msgObj, msgKey) in footerList" :key="msgKey">
                                        {{msgObj.message}}
                                    </p>
                                </template>
                             </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import DebugAssertionEvalForm from "./DebugAssertionEvalForm";
    import FHIRPathExpressionEditorForm from "./FHIRPathExpressionEditorForm";

    export default {
        data() {
            return {
                isDragging: false,
                drag_pos_left: 0,
                drag_pos_top: 0,
                // resizeObTimer: null,
                // resizeOb: null,
            }
        },
        mounted() {
        },
        props: ['show'],
        computed: {
            selectedPatternTypeId() {
               return this.$store.state.debugAssertionEval.selectedPatternTypeId
            },
            getSelectedPatternTypeObj() {
                let obj = this.$store.state.debugAssertionEval.evalObjByPattern.patternTypes[this.selectedPatternTypeId]
                return obj
            },
            footerList() {
                let obj = this.getSelectedPatternTypeObj
                if ('footerList' in obj) {
                   return obj.footerList
                } else {
                    return null
                }
            },
            wasEvaluatedAtLeastOnce() {
                // let obj = this.getSelectedPatternTypeObj
                let obj = this.getSelectedObj
                return obj.resultObj.wasEvaluatedAtleastOnce
            },
            hasFhirPathExpression() {
                return ('fhirPathContextObj' in this.getSelectedPatternTypeObj)
            },
            getSelectedObj() {
                if (this.hasFhirPathExpression && this.isFhirPathTabSelected) {
                    return this.getSelectedPatternTypeObj.fhirPathContextObj
                } else {
                    return this.getSelectedPatternTypeObj
                }
            },
            isFhirPathTabSelected() {
                return this.selectedEvalOptionTab === 'fhirPathTab'
            },
            selectedEvalOptionTab() {
                if ('selectedEvalOptionTab' in this.getSelectedPatternTypeObj) {
                    const selectedOptionTab = this.getSelectedPatternTypeObj.selectedEvalOptionTab
                    if (selectedOptionTab !== '')
                        return selectedOptionTab
                }
                return this.$store.state.debugAssertionEval.defaultEvalOptionTab
            },
            tabMap() {
                return this.$store.state.debugAssertionEval.tabMap
            }
        },
        methods: {
            isSelectedPatternType(patternTypeId) {
               return (this.selectedPatternTypeId === patternTypeId)
            },
            isSelectedOptionTab(tabId) {
               return (this.selectedEvalOptionTab === tabId)
            },
            doSelectEvalOptionTab(tabId) {
                this.$store.commit('setSelectedEvalOptionTab', tabId)
            },
            doSelectPatternTypeId(patternTypeId) {
                this.$store.commit('setSelectedPatternTypeId', patternTypeId)
                // If initial selection, then set optionTab to Assertion when pattern selection changes
                if ('selectedEvalOptionTab' in this.getSelectedPatternTypeObj)
                    if (this.getSelectedPatternTypeObj.selectedEvalOptionTab === '') {
                        this.$store.commit('setSelectedEvalOptionTab', this.$store.state.debugAssertionEval.defaultEvalOptionTab)
                    }
                // This is needed so when the selector is switched from a longer display to a smaller one (ie, one that contains fewer assert elements, the top portion is not hidden
                // document.querySelector('div#debugAssertionEvalModal').scrollTop = 0
                // Unset the height property so that the footer is not hidden at the bottom
                const elFlexContainer = document.querySelector('div.dafFlexContainer')
                const defaultPatternTypeId = this.$store.state.debugAssertionEval.defaultPatternTypeId
                if (patternTypeId === defaultPatternTypeId) {
                    elFlexContainer.style.height = "754px"
                } else {
                    elFlexContainer.style.height = "auto"
                }
            },
            close()  {
                /*
                if (this.resizeOb !== null) {
                    try {
                        let modalEl = document.querySelector('div#debugAssertionEvalModal.eval-modal-container')
                        this.resizeOb.unobserve(modalEl)
                        if (this.resizeObTimer !== null) {
                            clearTimeout(this.resizeObTimer)
                            this.resizeObTimer = null
                        }
                        this.resizeOb = null
                    } catch (e) {
                       // May not be supported in all browsers, like I.E.
                    }
                }
                */
                const defaultPatternTypeId = this.$store.state.debugAssertionEval.defaultPatternTypeId
                this.$store.commit('setDebugAssertionEvalPropKey', {patternTypeId: defaultPatternTypeId, propKey: ''})
                this.$emit('close')
            },
            doResume() {
                const defaultPatternTypeId = this.$store.state.debugAssertionEval.defaultPatternTypeId
                this.$store.commit('setDebugAssertionEvalPropKey', {patternTypeId: defaultPatternTypeId, propKey: ''})
                this.$emit('resume')
            },
            drag_start: function (event) {
                this.isDragging = true
                try {
                    var el = document.getElementById('debugAssertionEvalModal')
                    if (el.parentNode.classList.contains('modalFlexContainer'))
                        el.parentNode.classList.remove('modalFlexContainer') // remove the flex box centering so that we can apply a custom Left property
                    var rect = el.getBoundingClientRect();
                    const margin_top = 40 /* Drag and drop ghosting was fixed by using the exact top including this margin and separating the body and the header part as separate divs instead of one big container. Top margin is defined in the css for the eval-modal-container style */
                    this.drag_pos_left = rect.left - event.clientX
                    this.drag_pos_top = rect.top - event.clientY - margin_top
                    // el.style.opacity = ".1"
                    // el.style.border = "2px dashed blue" // This caused inner contents to shift due to the extra 2px border
                    // el.firstChild.style.opacity = "0" // This is helpful to avoid foreground image ghosting
                    // el.firstChild.style.visibility = "hidden" // This is helpful to avoid foreground image ghosting
                } catch (e) {
                    console.log(e)
                }
            },
            drop: function drop(event) {
                this.isDragging = false
                let el = document.getElementById('debugAssertionEvalModal')
                if (el) {
                    // el.style.opacity = "1"
                    el.style.border = "none"
                    // el.firstChild.style.opacity = "1"
                    // el.firstChild.style.visibility = "visible"
                }
                event.preventDefault();
                return false;
            },
            drag_over: function (event) {
                this.isDragging = false
                try {
                    let el = document.getElementById('debugAssertionEvalModal')
                    if (el) {
                        el.style.position = 'fixed'
                        el.style.left = parseInt(this.drag_pos_left + event.clientX) + 'px'
                        el.style.top = parseInt(this.drag_pos_top + event.clientY) + 'px'
                    }
                    event.preventDefault();
                } catch (e) {
                    console.log(e)
                }
                return false;
            },
        },
        components: {
            DebugAssertionEvalForm,
            FHIRPathExpressionEditorForm,
        },
        name: "DebugAssertionEvalModal"
    }

</script>


<style scoped>
    .evalOptionTabHeader {
        text-align: left;
        border-bottom: inset;
        margin-bottom: 10px;
    }
    .selectedEvalOptionTab,
    .evalOptionTab {
        background-color: lavenderblush;
        cursor: pointer;
        border-top-left-radius: 12px;
        border-top-right-radius: 12px;
        font-size: smaller;
        display: inline-block;
        margin-right: 10px;
        text-align: left;
        padding: 4px;
    }

    .selectedEvalOptionTab {
        background-color: plum;
        font-weight: bold;
    }

    .selectedPatternType,
    .selectPatternType {
        border: 1px solid gray;
        display: inline-block;
        cursor: pointer;
        background-color: lavender;
        font-weight: bolder;
        margin: 5px;
        text-align: left;
    }
    .selectedPatternType {
        background-color: plum;
    }
    .closeXIcon {
        position: relative;
        display: inline-block;
        left: 45%;
        text-align: right;
        cursor: pointer;
        font-size: small;
        margin-top: 2px;
        margin-left: 2px;
        vertical-align: text-top;
    }
    .patternSeparator {
        border: 1px solid #f5f5f5;
        visibility: hidden;
    }

    div#daemHeader {
        position: -webkit-sticky;
        position: sticky;
        background-color: white;
        width: 100%;
        top: 0;
        padding: 10px 10px 5px 10px;
        z-index: 9100;
    }

    .patternHeaderContainer {
        padding: 5px;
    }
    .patternHeader {
        text-align: left;
        display: block;
        font-weight: bolder;
        margin: 4px;
        background-color: inherit;
    }

    .patternHeaderButtons {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        width: auto;
        vertical-align: middle
    }

    /**
     This style centers the modal dialog on the screen (behind its modal mask)
     */
    .modalFlexContainer {
        display: flex;
        align-items: center;
        justify-content: space-evenly;
        z-index: 9000;
    }

    * {
        box-sizing: border-box;
    }

    .modal-mask {
        position: fixed;
        z-index: 8000;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, .3);
        /*transition: opacity 500ms ease;*/
    }

    .eval-modal-container {
        width: 760px; /* setting width seems to avoid ghosting problem when drag-n-drop is used for moving window */
        height: 854px;
        overflow-x: scroll;
        overflow-y: scroll;
        margin: 40px auto 0px;
        /*padding: 20px 30px;*/
        background-color: #ffffff;
        border-radius: 2px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, .33);
        font-family: Helvetica, Arial, sans-serif;
        resize: both;
        /*transition: opacity 100ms linear; */ /* This was helpful when drag and drop is used to move the dialog window before the ghosting was fixed */
        visibility: visible;
    }

    .eval-modal-header h3 {
        display: inline-block;
        margin-top: 0;
        color: #42b983;
        background-color: lavender;
        width: 100%;
        cursor: move;
        text-align: center;
    }

    .modal-footer,
    .modal-body {
        margin: 20px;
        /*margin-bottom: 20px;*/
    }


    /*
     * The following styles are auto-applied to elements with
     * transition="modal" when their visibility is toggled
     * by Vue.js.
     *
     * You can easily play with the modal transition by editing
     * these styles.
     */

    .modal-enter {
        opacity: .5;
    }

    .modal-leave-active {
        opacity: .5;
    }

    /*.modal-enter .modal-container,*/
    /*.modal-leave-active .modal-container {*/
    /*    -webkit-transform: scale(1.1);*/
    /*    transform: scale(1.1);*/
    /*}*/


</style>