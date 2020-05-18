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
                            <button class="runallbutton" @click.stop="doRun(name)">Run</button>
                            <button v-if="isDebuggable(i)"
                                        class="debugTestScriptButton"
                                        @click.stop="doDebug(name)">{{getDebugActionButtonLabel(i)}}</button>
                            <button v-if="isEvaluable(i)"
                                        class="debugTestScriptButton"
                                        @click.stop="doDebugEvalMode(name)">Eval</button>
                            <button v-if="isDebugKillable(i)"
                                        class="debugKillTestScriptButton"
                                        @click.stop="doDebugKill(i)">Kill</button>
                            <span v-if="$store.state.debugTestScript.waitingForBreakpoint">&nbsp;&nbsp;&#x23F1;</span> <!-- Display a stopwatch if waiting for breakpoint to be hit -->
                    </span>
                    <span v-if="! $store.state.debugTestScript.waitingForBreakpoint && ! $store.state.testRunner.isClientTest"> --  {{ testTime(name) }}</span>
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
            'channelId': function(newVal) {
                if (this.channel !== newVal)
                    this.channel = newVal
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
    .breakpointColumnHeader {
        position: absolute;
        left: 0px;
        font-size: 8px;
        text-decoration: underline;
    }
</style>
