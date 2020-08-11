<template>
    <div name="modal" @dragover="drag_over" @drop="drop">
        <div class="modal-mask" @click="close" v-show="show">
            <div class="modalFlexContainer">
                <div class="modal-container" @click.stop id="debugAssertionEvalModal" >
                    <div>
                        <div class="modal-header" draggable="true" @dragstart="drag_start">
                        <span style="font-size: small; text-align: right; position: relative; left: 50%; cursor: pointer"
                              title="Close" @click="close">&#x274c;</span>
                        <h3>Eval</h3>
                    </div>
                        <div class="modal-body">
                        <!--
                        <div v-for="(val, propKey) in $store.state.debugAssertionEval.evalObj" :key="propKey" >
                            <div><label  class="form-label"  :for="propKey">{{propKey}}</label><span @click.stop="openHelp(propKey)" class="inputLabelInformation">&#x2139;</span></div>
                                <input class="form-control" :id="propKey"  :value="getPropVal(propKey)" @input="onEvalObjPropUpdate">&nbsp;
                                <button class="modal-button" @click="doEval(propKey)">Eval</button>&nbsp;
                                <button class="modal-button" @click="doResume()" title="Resume execution or resume until next breakpoint">Resume</button>&nbsp;
                                <div v-if="getPropKey() === propKey"
                                     v-bind:class="{
                                    'evalNotPassed': getResultCode().valueOf() !== 'pass',
                                }">
                                    <span class="form-block">{{getResultCode()}}</span>
                                    <vue-markdown v-bind:source="$store.state.debugAssertionEval.debugAssertionEvalResult.markdownMessage"></vue-markdown>
                                </div>
                        </div>
                        -->

                        <div class="flexContainer">
                            <div v-for="(val, propKey) in $store.state.debugAssertionEval.evalObj" :key="propKey">
                                <div>
                                    <div><label :for="propKey">{{propKey}}</label><span @click.stop="openHelp(propKey)"
                                                                                        class="inputLabelInformation"
                                                                                        title="TestScript Element - Detailed Description">&#x2139;</span>
                                    </div>
                                    <div>
                                        <template v-if="isPropertyAnEnumType(propKey)">
                                            <select class="form-control-select" :title="getEnumTypeFormalDefinition(propKey)" > <!-- TODO: Remove title and make a new span below the label. -->
                                                <option v-for="option in getEnumTypeArray(propKey)" :value="option.codeValue" :title="option.definition" :key="option.codeValue">
                                                    {{ option.displayName }}
                                                </option>
                                            </select>
                                        </template>
                                        <template v-else>
                                             <textarea
                                                v-bind:class="{
                                                'form-control-textarea-error': getResultCode().valueOf() !== 'pass' && getPropKey() === propKey,
                                                'form-control-textarea' : true,
                                                }"
                                                   :id="propKey"
                                                   :value="getPropVal(propKey)" @input="onEvalObjPropUpdate" @keyup="evalOnKeyUp"/>
                                        </template>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div
                             v-bind:class="{
                                'evalNotPassed': getResultCode().valueOf() !== 'pass',
                            }">
                            <span class="form-block">{{getResultCode()}}</span>
                            <vue-markdown v-bind:source="$store.state.debugAssertionEval.debugAssertionEvalResult.markdownMessage"></vue-markdown>
                        </div>
                        </div>

                    <!--                <div class="modal-footer text-right">-->
                    <!--                </div>-->
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import VueMarkdown from 'vue-markdown'

    export default {
        data() {
            return {
                drag_pos_left: 0,
                drag_pos_top: 0,
                evalTimer: null,
            }
        },
        mounted() {
        },
        props: ['show'],
        computed: {},
        methods: {
            openHelp(propKey) {
                window.open("http://hl7.org/fhir/testscript-definitions.html#TestScript.setup.action.assert." + propKey, "_blank")
            },
            getResultCode() {
                return this.$store.state.debugAssertionEval.debugAssertionEvalResult.resultMessage
            },
            getPropKey() {
                return this.$store.state.debugAssertionEval.debugAssertionEvalResult.propKey
            },
            getPropVal(key) {
                return this.$store.state.debugAssertionEval.evalObj[key]
            },
            getEnumTypeFormalDefinition(propKey) {
                return this.$store.state.debugAssertionEval.enumValueTypes[propKey].formalDefinition
            },
            getEnumTypeArray(propKey) {
                return this.$store.state.debugAssertionEval.enumValueTypes[propKey].values
            },
            isPropertyAnEnumType(propKey) {
                if (this.$store.state.debugAssertionEval.enumValueTypes  !== null && propKey in this.$store.state.debugAssertionEval.enumValueTypes) {
                    return true
                }
                return false
            },
            onEvalObjPropUpdate(e) {
                // console.log('onEvalObjProp.. was called.')
                this.$store.commit('setEvalObjProperty', {propKey: e.target.id, propVal: e.target.value})
            },
            close: function () {
                this.$store.commit('setDebugAssertionEvalPropKey', '')
                this.$emit('close')
            },
            doEval(propKey) {
                this.$store.commit('setDebugAssertionEvalPropKey', propKey)
                let assertDataString = JSON.stringify(this.$store.state.debugAssertionEval.evalObj)
                // console.log('before base64: ' + assertDataString)
                this.$store.dispatch('doDebugEvalAssertion', window.btoa(assertDataString))
            },
            doResume: function () {
                this.$store.commit('setDebugAssertionEvalPropKey', '')
                this.$emit('resume')
            },
            doAdd: function () {

            },
            drag_start: function (event) {
                try {
                    var el = document.getElementById('debugAssertionEvalModal')
                    if (el.parentNode.classList.contains('modalFlexContainer'))
                        el.parentNode.classList.remove('modalFlexContainer') // remove the flex box centering so that we can apply a custom Left property
                    var rect = el.getBoundingClientRect();
                    this.drag_pos_left = rect.left - event.clientX
                    this.drag_pos_top = rect.top - event.clientY
                    el.style.opacity = ".1"
                    el.style.border = "1px solid blue"
                    el.firstChild.style.opacity = "0"
                    el.firstChild.style.visibility = "hidden"
                } catch (e) {
                    console.log(e)
                }
            },
            drop: function drop(event) {
                var el = document.getElementById('debugAssertionEvalModal')
                if (el) {
                    el.style.opacity = "1"
                    el.style.border = "none"
                    el.firstChild.style.opacity = "1"
                    el.firstChild.style.visibility = "visible"
                }
                event.preventDefault();
                return false;
            },
            drag_over: function (event) {
                try {
                    var el = document.getElementById('debugAssertionEvalModal')
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
            VueMarkdown
        },
        name: "DebugAssertionEval"
    }

</script>


<style scoped>
    .modalFlexContainer {
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .flexContainer {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        width: auto;
        height: 50%;
    }

    * {
        box-sizing: border-box;
    }

    .inputLabelInformation {
        vertical-align: top;
        font-size: small;
        cursor: pointer;
        margin-left: 4px;
        margin-bottom: 4px;
    }

    .modal-mask {
        position: fixed;
        z-index: 9998;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        /*background-color: rgba(0, 0, 0, .5);*/
        /*transition: opacity 500ms ease;*/
    }

    .modal-container {
        width: 40%;
        max-height: 80%;
        overflow-y: auto;
        margin: 40px auto 0;
        padding: 20px 30px;
        background-color: #ffffff;
        border-radius: 2px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, .33);
        font-family: Helvetica, Arial, sans-serif;
        resize: both;
        transition: opacity 100ms linear;
    }

    .modal-header h3 {
        margin-top: 0;
        color: #42b983;
        background-color: lavender;
        width: 100%;
        cursor: move;
    }

    .modal-body {
        margin: 20px 0;
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

    .evalNotPassed {
        color: red;
    }

</style>