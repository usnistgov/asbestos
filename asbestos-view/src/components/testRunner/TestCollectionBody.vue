<template>
    <div>
        <div class="runallgroup">
            <span v-if="running" class="running">Running</span>
            <div class="divider"></div>
            <div class="divider"></div>

            <span v-if="!$store.state.testRunner.isClientTest">
                <input type="checkbox" id="doGzip" v-model="gzip">
                <label for="doGzip">GZip?</label>
                <div class="divider"></div>
            </span>

            <button v-bind:class="{'button-selected': json, 'button-not-selected': !json}" @click="doJson()">JSON</button>
            <button v-bind:class="{'button-selected': !json, 'button-not-selected': json}" @click="doXml()">XML</button>
            <div class="divider"></div>
            <div class="divider"></div>
            <button class="runallbutton" @click="doRunAll()">Run All</button>
        </div>

        <h3 class="conformance-tests-header">Tests</h3>
        <div>
            <div class="testBarMargin" v-for="(name, i) in scriptNames"
                 :key="name + i" >
                <div v-bind:class="{
                                'pass': status[name] === 'pass' && colorful,
                                'pass-plain-header': status[name] === 'pass' && !colorful,
                                'fail': status[name] === 'fail' && colorful,
                                'fail-plain-header': status[name] === 'fail' && !colorful,
                                'error': status[name] === 'error',
                                'not-run':  status[name] === 'not-run' && colorful /*  !status[name] */,
                                'not-run-plain': status[name] === 'not-run' && ! colorful,
                           }" @click.prevent="openTest(name)">

                    <script-status v-if="!statusRight" :status-right="statusRight" :name="name"> </script-status>

                    <template v-if="isPreviousDebuggerStillAttached(i)">
                        <span class="breakpointColumnHeader" title="A debugger is running for this TestScript.">&#x1F41E;</span> <!-- lady beetle icon -->
                    </template>
                    <template v-else-if="$store.state.testRunner.currentTest === name && ! isDebuggable(i)">
                        <span class="breakpointColumnHeader" title="Add at least one breakpoint in the column below to enable debugging.">&#x2139;</span> <!-- the "i" Information icon -->
                    </template>

                    <span v-if="$store.state.testRunner.currentTest === name">
                            <img src="../../assets/arrow-down.png">
                    </span>
                    <span v-else>
                            <img src="../../assets/arrow-right.png"/>
                    </span>
                    <span class="large-text">{{ cleanTestName(name) }}</span>

                    <span v-if="isClient">
                            <button class="runallbutton" @click="doEval(name)">Run</button>
                    </span>
                    <span v-else>
                          <template v-if="isPreviousDebuggerStillAttached(i)">
                                <button
                                        @click.stop="removeDebugger(i)"
                                        class="stopDebugTestScriptButton">Remove Debugger</button>
                            </template>
                            <template v-else>
                                <button v-if="! isResumable(i)" @click.stop="doRun(name)" class="runallbutton">Run</button>
                                <template v-if="$store.state.testRunner.currentTest === name">
                                    <button v-if="isDebuggable(i)"
                                            @click.stop="doDebug(name)"
                                            class="debugTestScriptButton"
                                            >Debug</button>
                                    <button v-if="isResumable(i)"
                                            :disabled="isWaitingForBreakpoint"
                                            @click.stop="doDebug(name)"
                                            class="debugTestScriptButtonNormal"
                                            >&#x25B6;&nbsp;Resume</button>
                                    <button v-if="isResumable(i)"
                                            :disabled="isWaitingForBreakpoint"
                                            @click.stop="doStepOver(i)"
                                            class="debugTestScriptButtonNormal"
                                        >&#x2935; Step Over</button>
                                    <button v-if="isDebuggable(i) || isResumable(i)"
                                            :disabled="isWaitingForBreakpoint"
                                            title="Clear all breakpoints."
                                            @click.stop="removeAllBreakpoints(i)"
                                            class="debugTestScriptButtonNormal">&#x274E; Clear BPs.</button> <!-- &#x1F191; CL button -->
                                    <button v-if="isResumable(i)"
                                            :disabled="isWaitingForBreakpoint"
                                            @click.stop="stopDebugging(i)"
                                        class="debugTestScriptButtonNormal">&#x1F7E5; Stop</button> <!-- &#x270B; -->
                                    <span v-if="isWaitingForBreakpoint">&nbsp;&nbsp;&#x23F1;</span>
                                    <!-- Display a stopwatch if waiting for breakpoint to be hit -->
                                </template>
                            </template>
                    </span>
                    <span v-if="! isWaitingForBreakpoint && ! $store.state.testRunner.isClientTest"> --  {{ testTime(name) }}</span>
                </div>
                <debug-assertion-eval v-if="isEvaluable(i)" :show="$store.state.debugAssertionEval.showModal" @close="closeModal()" @resume="doDebug(name)"></debug-assertion-eval>
                <router-view v-if="selected === name"></router-view>  <!--  opens TestOrEvalDetails   -->
            </div>
        </div>
    </div>
</template>

<script>
    import testCollectionMgmt from "../../mixins/testCollectionMgmt";
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import debugTestScriptMixin from "../../mixins/debugTestScript";
    import ScriptStatus from "./ScriptStatus";
    import DebugAssertionEval from "./debugger/DebugAssertionEval";

    export default {
        methods: {
            load() {
                this.loadTestCollection(this.testCollection)
                this.$store.dispatch('debugMgmt', {'cmd':'getExistingDebuggerList'})
            },
            openTest(name) {
                if (!name)
                    return
                if (this.selected === name)  { // unselect
                    this.$store.commit('setCurrentTest', null)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`
                    this.$router.push(route)
                    return
                }
                this.$store.commit('setCurrentTest', name)
                const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${name}`
                this.$router.push(route)
            },
        },
        computed: {
            selected() {
                return this.$store.state.testRunner.currentTest
            },
        },
        created() {
            this.load(this.testCollection)
            this.channel = this.channelId
            this.setEvalCount()
        },
        watch: {
            'evalCount': 'setEvalCount',
            'testCollection': 'load',
            'channelId': function() {
                this.load();
            },
        },
        mixins: [ testCollectionMgmt, colorizeTestReports, debugTestScriptMixin, ],
        name: "TestCollectionBody",
        props: [
            'sessionId', 'channelId', 'testCollection',
        ],
        components: {
            ScriptStatus,
            DebugAssertionEval
        }
    }
</script>

<style scoped>
</style>
<style>
    .breakpointColumnHeader {
        position: absolute;
        left: 5px;
        font-size: 14px;
        font-weight: normal;
        text-decoration: none;
        cursor: default;
    }
    .debugTestScriptButtonNormal,
    .debugTestScriptButton {
        margin-left: 10px;
        background-color: cornflowerblue;
        cursor: pointer;
        border-radius: 25px;
        font-weight: bold;
    }
    .debugTestScriptButtonNormal {
        font-weight: normal;
    }
    .debugFeatureOptionButton {
        margin-left: 7px;
        border-radius: 3px;
        background-color: #FFC83D;
       font-size: x-small;
    }
</style>
