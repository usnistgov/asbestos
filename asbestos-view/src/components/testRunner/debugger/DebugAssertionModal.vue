<template>
    <div name="modal" @dragover="drag_over" @drop="drop">
        <div class="modal-mask" @click="close" v-show="show">
            <div class="modalFlexContainer">
                <div class="eval-modal-container" @click.stop id="debugAssertionEvalModal">
                    <div>
                        <div>
                        <span class="eval-modal-header" draggable="true" @dragstart="drag_start">
                            <h3>Eval<span class="closeXIcon" title="Close" @click="close">&#x274c;
                            </span></h3>
                        </span>
                        </div>

                        <div class="modal-body">
                            <div class="patternHeader">Select a Pattern:</div>
                            <div style="display: flex;  flex-direction: row;  flex-wrap: wrap;  width: auto; vertical-align: middle">
                                <div v-for="(val, patternType) in $store.state.debugAssertionEval.evalObjByPattern.patternTypes"
                                 :key="patternType">
                                    <div
                                    v-bind:class="{
                                            'selectedPatternType': isSelectedPatternType(patternType),
                                            'selectPatternType' : true,
                                            }"
                                    @click="doSelectPatternTypeId(patternType)">{{patternType}}</div>
                                </div>
                            </div>
                            <div style="margin: 6px">
                                 <debug-assertion-form :pattern-type-id="selectedPatternTypeId" :is-shown="show"/>
                            </div>
                        </div>

                        <!--   <div class="modal-footer text-right">-->
                        <!--   </div>-->
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import DebugAssertionForm from "./DebugAssertionForm";

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
        computed: {
            selectedPatternTypeId() {
               return this.$store.state.debugAssertionEval.selectedPatternTypeId
            }
        },
        methods: {
            isSelectedPatternType(patternTypeId) {
               return (this.selectedPatternTypeId === patternTypeId)
            },
            doSelectPatternTypeId(patternTypeId) {
                this.$store.commit('setSelectedPatternTypeId', patternTypeId)
            },
            close: function () {
                this.$store.commit('setDebugAssertionEvalPropKey', {patternTypeId: 'OriginalAssertion', propKey: ''})
                this.$emit('close')
            },
            doResume: function () {
                this.$store.commit('setDebugAssertionEvalPropKey', {patternTypeId: 'OriginalAssertion', propKey: ''})
                this.$emit('resume')
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
        },
        components: {
            DebugAssertionForm
        },
        name: "DebugAssertionEval"
    }

</script>


<style scoped>
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
        vertical-align: text-top;
    }
    .patternSeparator {
        border: 1px solid #f5f5f5;
        visibility: hidden;
    }

    .patternHeader {
        text-align: left;
        display: block;
        font-weight: bolder;
        margin: 4px;
        background-color: inherit;
    }

    .modalFlexContainer {
        display: flex;
        align-items: center;
        justify-content: space-evenly;
    }


    * {
        box-sizing: border-box;
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

    .eval-modal-container {
        width: auto;
        height: auto;
        max-height: 80%;
        /*width: 42%;*/
        /*height: 60%;*/
        overflow-x: scroll;
        overflow-y: scroll;
        margin: 40px auto 0;
        padding: 20px 30px;
        background-color: #ffffff;
        border-radius: 2px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, .33);
        font-family: Helvetica, Arial, sans-serif;
        resize: both;
        transition: opacity 100ms linear;
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

    .modal-body {
        margin-bottom: 20px;
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