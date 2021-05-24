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

          <template v-if="! isClient">
            <template v-if="isDebugFeatureEnabled">
              <template v-if="isPreviousDebuggerStillAttached(i)">
                <span class="breakpointColumnHeader" title="A debugger is running for this TestScript.">&#x1F41E;</span> <!-- lady beetle icon -->
              </template>
              <template v-else-if="$store.state.testRunner.currentTest === name && ! isDebuggable(i) && ! isResumable(i)">
                <span class="breakpointColumnHeader infoIcon" title="Add at least one breakpoint in the column below to enable debugging.">&nbsp;&#x2139;</span> <!-- the "i" Information icon -->
              </template>
              <template v-else-if="$store.state.testRunner.currentTest === name && (isDebuggable(i) || isResumable(i))">
                <span class="breakpointColumnHeader clickableColumnHeader" title="Clear all breakpoints." @click.stop="removeAllBreakpoints(i)">&#x1F191;</span> <!-- the "i" Information icon -->
              </template>
            </template>
          </template>

          <span v-if="selected === name">
                            <img src="../../assets/arrow-down.png">
          </span>
          <span v-else>
                            <img src="../../assets/arrow-right.png"/>
          </span>
          <span class="large-text">{{ cleanTestName(name) }}</span>
          &nbsp;
          <span v-if="isClient">
                            <button class="runallbutton" @click="doEval(name)">Run</button>
          </span>
          <span v-else-if="isDebugFeatureEnabled">
                          <template v-if="isPreviousDebuggerStillAttached(i)">
                                <button
                                    @click.stop="removeDebugger(i)"
                                    class="stopDebugTestScriptButton">Remove Debugger</button>
                            </template>
                            <template v-else>
                                <button v-if="! isResumable(i) && ! isWaitingForBreakpoint" @click.stop="doRun(name, testRoutePath)" class="runallbutton">Run</button>
                                <template v-if="$store.state.testRunner.currentTest === name">
                                    <button v-if="isDebuggable(i) && ! isWaitingForBreakpoint"
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
                                     <button v-if="isResumable(i)"
                                             :disabled="isWaitingForBreakpoint"
                                             title="Continue running and skip all breakpoints."
                                             @click.stop="doFinish(i)"
                                             class="debugTestScriptButtonNormal">&#x23E9; Skip All BPs.</button>
                                    <button v-if="isResumable(i)"
                                            :disabled="isWaitingForBreakpoint"
                                            @click.stop="stopDebugging(i)"
                                        class="debugTestScriptButtonNormal">&#x1F7E5; Stop</button> <!-- &#x270B; -->
                                    <span v-if="isWaitingForBreakpoint">&nbsp;Please wait...&nbsp;&#x23F1;</span>
                                    <!-- Display a stopwatch if waiting for breakpoint to be hit -->
                                </template>
                            </template>
                    </span>
          <span v-else>
                          <button @click.stop="doRun(name, testRoutePath)" class="runallbutton">Run</button>
          </span>
          <span v-if="! isWaitingForBreakpoint && ! $store.state.testRunner.isClientTest"> --  {{ testTime(name) }}</span>
        </div>
        <debug-assertion-eval-modal v-if="isDebugFeatureEnabled && isEvaluableAction(i)" :show="$store.state.debugAssertionEval.showEvalModalDialog" @close="closeModal()" @resume="doDebug(name)"></debug-assertion-eval-modal>
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
import DebugAssertionEvalModal from "./debugger/DebugAssertionEvalModal";

export default {
  methods: {
    load() {
      /*
       All tests details will be collapsed when loaded.
       The following setCurrentTest to null will reset the expanded arrow indicator
       otherwise the arrow indicator is incorrect when navigating out of the test collection and back into to the same test collection after a test was previously opened.
       */
      this.$store.commit('setCurrentTest', null)
      this.loadTestCollection(this.testCollection).then(() => {
          if (this.isClient === false) {
              this.$store.dispatch('debugMgmt', {'cmd': 'getExistingDebuggerList'})
          }
          const testIdParam = this.$router.currentRoute.params['testId']
          if (testIdParam !== undefined && testIdParam !== null) {
              this.$store.commit('setCurrentTest', testIdParam)
          }
      })
    },
    openTest(name) {
      if (!name)
        return
      if (this.selected === name)  { // unselect
        this.$store.commit('setCurrentTest', null)
        const route = `/session/${this.sessionId}/channel/${this.channelName}/collection/${this.testCollection}`
        this.$router.push(route)
        return
      }
      this.$store.commit('setCurrentTest', name)
      const selectedRoutePath = `${this.testRoutePath}/${this.selected}`
      this.$router.push(selectedRoutePath)
    },
  },
  computed: {
    selected() {
      return this.$store.state.testRunner.currentTest
    },
    testRoutePath() {
      const route = `/session/${this.sessionId}/channel/${this.channelName}/collection/${this.testCollection}/test`
      return route
    },
  },
  created() {
      if (this.$store.state.base.ftkChannelLoaded) {
          this.load(this.testCollection)
          // this.channel = this.channelName
          this.setEvalCount()
      } else {
          console.log('on create, ftkChannelLoaded is false')
      }
  },
  mounted() {
        this.$store.subscribe((mutation) => {
            if (mutation.type === 'ftkChannelLoaded') {
                if (this.$store.state.base.ftkChannelLoaded) {
                    // console.log('TestCollectionBody syncing on mutation.type: ' + mutation.type)
                    this.load(this.testCollection)
                    this.setEvalCount()
                }
            }
        })
  },
  watch: {
    'evalCount': 'setEvalCount',
    'testCollection': 'load',
    // 'channelName': function() {
    //   this.load();
    // },
  },
  mixins: [ testCollectionMgmt, colorizeTestReports, debugTestScriptMixin, ],
  name: "TestCollectionBody",
  props: [
    'sessionId', 'channelName', 'testCollection',
  ],
  components: {
    ScriptStatus,
    DebugAssertionEvalModal
  }
}
</script>

<style scoped>
</style>
<style>
.clickableColumnHeader,
.infoIcon,
.breakpointColumnHeader {
  position: absolute;
  left: 5px;
  font-size: 14px;
  font-weight: normal;
  text-decoration: none;
  cursor: default;
}
.infoIcon {
  width: 14px;
  border: lightgray 1px solid;
}
.clickableColumnHeader {
  cursor: pointer;
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
  background-color: lavender; /* #FFC83D; */
  font-size: x-small;
}
</style>
