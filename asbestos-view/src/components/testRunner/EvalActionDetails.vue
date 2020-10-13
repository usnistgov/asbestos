<template>
  <div>
    <div v-if="scriptAction && reportAction">
      <div @click.self="select()" v-bind:class="{
                                    'evalpass': result === 'pass',
                                    'evalfail': result === 'fail',
                                    'evalerror': result === 'error',
                                    'evalnotrun': result === 'not-run'
                            }">

        <test-status v-if="!statusRight"
                     :status-on-right="statusRight"
                     :report="report"
                     :script="script"
        > </test-status>

        <span class="selectable" @click.self="select()"> {{ description }}</span>

        <span v-if="open">
                <img src="../../assets/arrow-down.png" @click.self="select()">
            </span>
        <span v-else>
                <img src="../../assets/arrow-right.png" @click.self="select()">
            </span>
      </div>

      <div v-if="open">
        <div v-if="message && message.indexOf('#') === -1">
          <ul>
            <div v-for="(line, linei) in translateNL(message)"
                 :key="'msgDisp' + linei">
              <li>
                {{ line }}
              </li>
            </div>
          </ul>
        </div>

        <!-- Test Script/Report -->
        <div>
          <span class="selectable" @click="toggleScriptDisplayed()">Test Script/Report</span>
          <span v-if="displayScript">
                    <img src="../../assets/arrow-down.png" @click="toggleScriptDisplayed()">
                   <vue-markdown v-if="actionContext">{{actionContext}}</vue-markdown>
                    <script-display
                        :script="script"
                        :report="report">
                    </script-display>
                </span>
          <span v-else>
                    <img src="../../assets/arrow-right.png" @click="toggleScriptDisplayed()">
                </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import colorizeTestReports from "../../mixins/colorizeTestReports";
import TestStatus from "./TestStatus";
import VueMarkdown from 'vue-markdown';
import ScriptDisplay from "./ScriptDisplay";

export default {
  data() {
    return {
      open: false,
      displayScript: true,
    }
  },
  methods: {
    toggleScriptDisplayed() {
      this.displayScript = !this.displayScript
    },
    translateNL(string) {
      if (!string)
        return string
      return string.replace(/\t/g, "  ").split('\n')
    },
    select() {
      this.open = !this.open;
    },
    getExtensionValue(root, url) {
      const ext = this.getExtension(root, url);
      if (!ext) return null;
      return ext.valueString;
    },
    getExtension(root, url) {
      if (!root || !root.extension) return null;
      let ext = null;
      root.extension.forEach(e => {
        if (e.url === url)
          ext = e;
      })
      return ext;
    },
  },
  computed: {
    scriptAction() {
      return this.script;
    },
    reportAction() {
      return this.report;
    },
    message() {
      if (!this.report) return null;
      if (this.report.assert) return this.report.assert.message;
      if (this.report.operation) return this.report.operation.message;
      return null;
    },
    actionContext() {
      if (!this.report) return null;
      if (this.report.assert) return this.getExtensionValue(this.report.assert, "urn:action-context");
      if (this.report.operation) return this.getExtensionValue(this.report.operation, "urn:action-context");
      return null;
    },
    result() {
      if (this.report.assert) return this.report.assert.result;
      return null;
    },
    description() {
      let rawDesc;
      if (this.script.assert) rawDesc = this.script.assert.description;
      if (this.script.operation) rawDesc = this.script.operation.description;
      if (!rawDesc.includes("|"))
        return rawDesc;
      const elements = rawDesc.split("|");
      return elements[0];
    }
  },
  components: {
    TestStatus,
    VueMarkdown,
    ScriptDisplay
  },
  mixins: [ colorizeTestReports ],
  props: [
    'script', 'report'    // action parts
  ],
  name: "EvalActionDetails"
}
</script>

<style scoped>
.evalpass {
  text-align: left;
  /*border-top: 1px solid black;*/
  cursor: pointer;
}
.evalfail {
  text-align: left;
  /*border-top: 1px solid black;*/
  cursor: pointer;
}
.evalerror {
  text-align: left;
  /*border-top: 1px solid black;*/
  cursor: pointer;
}
.evalnotrun {
  text-align: left;
  /*border-top: 1px solid black;*/
  cursor: pointer;
}
</style>
