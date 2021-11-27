<template>
  <div>
    <span v-if="displayTestScript">
      <img src="../../assets/arrow-down.png">
    </span>
    <span v-else>
      <img src="../../assets/arrow-right.png"/>
    </span>
    <span class="selectable" @click.stop="toggleDisplay">View TestScript</span>

    <div v-if="displayTestScript">
    <div class="container">
      <div v-if="callingScript" class="script-header">Calling Script</div>
      <div v-if="callingScript" class="script"><pre>{{JSON.stringify(callingScript, null, 2)}}</pre></div>
      <div v-if="moduleScript" class="module-script-header">
          Module Script Header
      </div>
      <div v-if="moduleScript" class="module-script"><pre>{{JSON.stringify(moduleScriptHeader, null, 2)}}</pre></div>
      <div class="script-header2">
          <span v-if="callingScript">
            Module Script
          </span>
          <span v-else>
            Script
          </span>
      </div>
       <div class="report-header2">
            Report
        </div>
        <div class="script2"><pre>{{JSON.stringify( script, null, 2) }}</pre></div>
        <div class="report2"><pre>{{JSON.stringify( filteredReport, null, 2) }}</pre></div>
    </div>
  </div>
  </div>
</template>

<script>
    export default {
      data() {
          return {
            displayTestScript: false
          }
      },
        methods: {
          toggleDisplay() {
            this.displayTestScript = !this.displayTestScript;
          }
        },
        computed: {
          hasModuleScriptHeader() {
              return this.moduleScript && this.moduleScript.modifierExtension
          },
          moduleScriptHeader() {
            //  if (this.moduleScript && this.moduleScript.modifierExtension)
                return this.moduleScript.modifierExtension[0];
          },
            filteredReport() {
              if (this.report !== undefined && this.report !== '') {
                let clone = JSON.parse(JSON.stringify(this.report));
                if (clone !== null) {
                  if (clone.operation)
                    clone.operation.extension = undefined;
                  if (clone.assert)
                    clone.assert.extension = undefined;
                  return clone;
                }
              }
              return ''
            }
        },
        props: [
            // parts representing a single action
            'script', 'report', 'callingScript', 'moduleScript'
        ],
        components: {
        },
        name: "ScriptDisplay"
    }
</script>

<style scoped>
    .container {
        display: grid;
        grid-template-columns: auto auto;
      justify-items: start;
      justify-content: start;
      margin-top: 10px;
        margin-left: 10px;
      margin-bottom: 10px;
        text-indent: 0px;
    }
    .script-header {
        grid-column: 1 / span 1;
        grid-row: 1 / span 1;
        font-weight: bold;
    }
    .module-script-header {
        grid-column: 2 / span 1;
        grid-row: 1 / span 1;
        font-weight: bold;
    }
    .script-header2 {
        grid-column: 1 / span 1;
        grid-row: 3 / span 1;
        font-weight: bold;
    }


    .report-header2 {
        grid-column: 2 / span 1;
        grid-row: 3 / span 1;
        font-weight: bold;
    }
    .script2 {
        grid-column: 1;
        grid-row: 4;
    }
    .report2 {
        grid-column: 2;
        grid-row: 4;
    }
    .script {
        grid-column: 1 / span 1;
        grid-row: 2;
        word-wrap: break-word;
      font-weight: normal;
      border-color: black;
      border-style: solid;
      border: 1px;
    }
    .module-script {
        grid-column: 2 / span 1;
        grid-row: 2;
        word-wrap: break-word;
    }

</style>
