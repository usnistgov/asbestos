<template>
    <div>

      <div class="text-wrap-break-word">
        <div class="vdivider"></div>
        <div class="vdivider"></div>
        <div class="vdivider"></div>
        <div class="vdivider"></div>
        <div class="vdivider"></div>
        <div class="vdivider"></div>
          <span class="bold" title="Displays the TestScripts in order of execution. Imported test module components and aggregate module are all flattened (module nesting is deconstructed into a single-file structure). The end-result TestScript is basically conformant to the standard FHIR TestScript."><span class="scriptInfoIcon">&#x2139;</span>Script(s):&nbsp;</span>
        <span class="script-panel selectable underline"  @click="openScriptDisplay(testId)">&#x1F5D0;&nbsp;{{ testId }}</span>
          <span v-if="Object.getOwnPropertyNames(testModules).length > 0" @click="isScriptsClosed=!isScriptsClosed" :title="'Click to ' + (isScriptsClosed?'open':'hide') + ' all script modules'" class="moreScriptsLink">...</span>
          <span :class="[isScriptsClosed?'scriptsClosed':'scriptsOpen']">
        <span v-for="(name, namei) in Object.getOwnPropertyNames(testModules)"
             :key="'TestModule' + namei">
           <span class="script-panel selectable underline" @click="openScriptDisplay(name)">&#x1F5CF;&nbsp;{{ name }}</span>
        </span>
          </span>
      </div>

      <ul class="noTopMargin">
        <li v-if="isEventBasedDisplayMode">
            <div v-if="theScript !== undefined">
                <div class="instruction">
                   <vue-markdown>{{ theScript.description }}</vue-markdown>
                </div>
                <div v-if="eventIds === null">
                    No messages present on this channel
                </div>
                <div v-else>
                    <ul class="noListStyle">
                    <li v-for="(eventId, eventi) in eventIds"
                         :key="'Disp' + eventi">
                        <client-details
                                :session-id="sessionId"
                                :channel-name="channelName"
                                :test-collection="testCollection"
                                :test-id="testId"
                                :event-id="eventId"></client-details>
                    </li>
                    </ul>
                </div>
            </div>
        </li>
        <li v-else-if="theScript !== undefined && theScript !== null">
            <script-details
                :script="theScript"
                :report="theReport"
                :test-script-index="theTestScriptIndex"
                :disable-debugger="disableDebugger"
            > </script-details>
        </li>
        <li v-else>
            <span class="configurationError">Invalid TestScript: TestScript could not be loaded. Please fix the TestScript and reload the Test Collection using the Refresh image link.</span>
        </li>
    </ul>
    </div>
</template>

<script>
    import ScriptDetails from './ScriptDetails'
    import ClientDetails from './ClientDetails'
    import VueMarkdown from "vue-markdown";
    export default {
        data() {
              return {
               isScriptsClosed: true
            }
        },
        computed: {
          theScript() {
              if (this.testId.includes('/')) {
                  if (!(this.testId in this.$store.state.testRunner.moduleTestScripts)) {
                      console.error(this.testId + ' does not exist in moduleTestScripts object.')
                  }
                  return this.$store.state.testRunner.moduleTestScripts[this.testId]
              } else {
                  if (!(this.testId in this.$store.state.testRunner.testScripts)) {
                      console.error(this.testId + ' does not exist in testScripts object.')
                  }
                  return this.$store.state.testRunner.testScripts[this.testId] // this.$store.state.testRunner.currentTest
              }
          },
          theReport() {
              if (this.testId.includes('/')) {
                  if (! (this.testId in this.$store.state.testRunner.moduleTestReports)) {
                      console.error(this.testId + ' does not exist in moduleTestReports object.')
                  }
                  return this.$store.state.testRunner.moduleTestReports[this.testId]
              } else {
                  if (! (this.testId in this.$store.state.testRunner.testReports)) {
                      console.error(this.testId + ' does not exist in testReports object.')
                  }
                  return this.$store.state.testRunner.testReports[this.testId]  // $store.state.testRunner.currentTest
              }
          },
          theTestScriptIndex() {
              try {
                  if (this.testId.includes('/'))
                      return '' // not applicable for aggregate testscript
                  else
                      return this.$store.getters.allServerTestCollectionNames.indexOf(this.testCollection) + '.' + this.$store.state.testRunner.testScriptNames.indexOf(this.testId)
              } catch (e) {
                 console.error('theTestScriptIndex error:' + e)
                  return ''
              }
          },
          testModules() {
            const modules = {};
            const allModules = this.$store.state.testRunner.moduleTestScripts;
            const currentTest = this.testId;
            const prefix = currentTest + "/";
            for (const name in allModules) {
              if (name.startsWith(prefix)) {
                modules[name] = allModules[name];
              }
            }
            return modules;
          },
          description() {
                if (!this.$store.state.testRunner.testScripts) return null
                if (!this.$store.state.testRunner.testScripts[this.testId].description) return null
                return this.$store.state.testRunner.testScripts[this.testId].description.replace(/\n/g, "<br />")
            },
            /*
            testScript() {
                console.info('loading testScript for ' + this.testId)
                console.info('is testscript locally available? ' + (this.theScript !== undefined && this.theScript !== null))
                console.info('ts desc: ' + this.theScript.description)
                return this.theScript
                // this.loadTestScript();
                // return this.$store.state.testRunner.testScripts[this.testId]
            },
             */
            eventIds() {
                if (this.$store.state.testRunner.clientTestResult === undefined) return null
                if (! (this.testId in this.$store.state.testRunner.clientTestResult)) return null
                return Object.keys(this.$store.state.testRunner.clientTestResult[this.testId])
            },
            isEventBasedDisplayMode() {
              if (this.isAggregateDetail==='true') {
                  return false
              } else
                  return this.$store.state.testRunner.isClientTest
            }
        },
        methods: {
          openScriptDisplay(name) {
              /*
              The name parameter is not straightforward, the server call only uses the parent script name with the GetTestScriptRequest call,
              which returns all test scripts, including all module scripts, then based on the name parameter here, the front-end map is used to display the script.
               */
              console.info(`Open ${name}`)
              const scriptUrl = `/script/collection/${this.testCollection}/test`
              let localTestId = this.testId // The top-level test script links
              if (localTestId.includes('/')) {
                  // This handles the case where aggregate module script link shows up when a test-level node is expanded
                  localTestId = localTestId.split('/',2)[0]
              }
              window.open(`${scriptUrl}/${localTestId}/${name}`, "_blank");
          },
            async loadEventSummariesAndReRun() {
                await this.$store.dispatch('loadEventSummaries', {session: this.sessionId, channel: this.channelName})
                await this.$store.dispatch('runEval', this.testId);
            },
            async loadTestScript() {
                if (this.$store.state.testRunner.testScripts[this.testId] === null)
                    await this.$store.dispatch('loadTestScript', {testCollection: this.testCollection, testId: this.testId});
            }
        },
      created() {
          // console.log('TestOrEval... currentTest: ' + this.$store.state.testRunner.currentTest)
          //   console.log('TestOrEval... testScripts[] ' + this.$store.state.testRunner.testScripts[this.$store.state.testRunner.currentTest])
        // this.$store.commit('setCurrentTest', this.testId);
        // this.$store.commit('setCurrentTestCollection', this.testCollection);
      },
        watch: {
            '$store.state.base.channelName': 'loadEventSummariesAndReRun'
        },
        props: [
            'sessionId', 'channelName', 'testCollection', 'testId', 'disableDebugger', 'isAggregateDetail'
        ],
        components: {
            ScriptDetails, ClientDetails, VueMarkdown,
        },
        mounted() {
            if (this.$store.state.testRunner.testAssertions === null)
                this.$store.dispatch('loadTestAssertions')
        },
        name: "TestOrEvalDetails"
    }
</script>

<style scoped>
    .script-panel {
        padding-left: 20px;
        padding-right: 20px;
    }
    .text-wrap-break-word {
        word-wrap: break-word;
    }
    .scriptInfoIcon {
        margin: 4px;
        border: lightgray 1px solid;
    }
    .scriptsClosed {
        display: none;
        visibility: hidden;
    }
    .scriptsOpen {
        display: block;
        visibility: visible;
    }
    .moreScriptsLink {
        cursor: pointer;
    }
</style>
