<template>
    <div name="modal">
        <div class="modal-mask" @click="close" v-show="show">
            <div class="modal-container" @click.stop>
                <div class="modal-header">
                    <h3>Eval</h3>
                </div>
                <div class="modal-body">
<!--                    <label class="form-label">-->
<!--                        Title-->
<!--                        <input class="form-control">-->
<!--                    </label>-->
<!--                    <label class="form-label">-->
<!--                        Body-->
<!--                        <textarea rows="5" class="form-control"></textarea>-->
<!--                    </label>-->

                    <!-- begin -->
                    <div v-for="(val, propKey) in $store.state.debugAssertionEval.evalObj" :key="propKey" >
<!--                        <div v-if="! wasEdited && '' === getPropVal(propKey).valueOf()">-->
<!--                            <span class="form-label form-block">{{propKey}}</span>&nbsp;-->
<!--                            <span class="grayText">(No value)</span>&nbsp;-->
<!--                            <button class="modal-button" @click="doAdd()">Add</button>-->
<!--                        </div>-->
<!--                        <div v-else-if="getPropVal(propKey).valueOf().length > 0">-->
                            <label class="form-label form-block"  :for="propKey">{{propKey}}</label>
<!--                            <textarea  rows="5" class="form-control" :id="propKey"  :value="getPropVal(propKey)" @input="onEvalObjPropUpdate"/>-->
                            <input class="form-control" :id="propKey"  :value="getPropVal(propKey)" @input="onEvalObjPropUpdate">
                            <button class="modal-button" @click="doEval(propKey)">Eval</button>&nbsp;
                            <button class="modal-button" @click="doResume()" title="Resume execution or resume until next breakpoint">Resume</button>&nbsp;
                            <div v-if="getPropKey() === propKey"
                                 v-bind:class="{
                                'evalNotPassed': getResultCode().valueOf() !== 'pass',
                            }">
                                <span class="form-block">{{getResultCode()}}</span> <!-- TestReport.SetupActionAssertComponent getResult code -->
                                <vue-markdown v-bind:source="$store.state.debugAssertionEval.debugAssertionEvalResult.markdownMessage"></vue-markdown>
                            </div>
<!--                        </div>-->
                    </div>
                    <!-- end -->
                </div>

<!--                <div class="modal-footer text-right">-->
<!--                    <button class="modal-button" @click="doResume()">Resume</button>&nbsp;-->
<!--                    <button class="modal-default-button" @click="doEval()">-->
<!--                        Save-->
<!--                    </button>-->
<!--                </div>-->
            </div>
        </div>
    </div>
</template>

<script>
    import VueMarkdown from 'vue-markdown'

    export default {
        data() {
            return {
                // wasEdited: false,
            }
        },
        mounted() {
        },
        props: ['show'],
        computed: {
        },
        methods: {
            getResultCode() {
                return this.$store.state.debugAssertionEval.debugAssertionEvalResult.resultMessage
            },
            getPropKey() {
                return this.$store.state.debugAssertionEval.debugAssertionEvalResult.propKey
            },
            getPropVal(key) {
               return this.$store.state.debugAssertionEval.evalObj[key]
            },
            onEvalObjPropUpdate(e) {
                // this.wasEdited = true
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
               console.log('before base64: ' + assertDataString)
                this.$store.dispatch('doDebugEvalAssertion', window.btoa(assertDataString))
                // this.close()
            },
            doResume: function() {
                this.$store.commit('setDebugAssertionEvalPropKey', '')
                this.$emit('resume')
            },
            doAdd: function() {

            }
        },
        components: {
            VueMarkdown
        },
        name: "DebugAssertionEval"
    }

</script>


<style scoped>
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

    .modal-container {
        width: 40%;
        max-height: 70%;
        overflow-y: auto;
        margin: 40px auto 0;
        padding: 20px 30px;
        background-color: #fff;
        border-radius: 2px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, .33);
        /*transition: all 500ms ease;*/
        font-family: Helvetica, Arial, sans-serif;
    }

    .modal-header h3 {
        margin-top: 0;
        color: #42b983;
    }

    .modal-body {
        margin: 20px 0;
    }

    .text-right {
        text-align: right;
    }

    .form-label {
        margin-bottom: 1em;
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