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
      <div v-if="callingScript">
        <div class="script-header">
          Calling Script
        </div>
        <div class="script">
          <vue-json-pretty :data="callingScript"></vue-json-pretty>
        </div>
      </div>
      <div v-if="moduleScript">
        <div class="script-header">
          Module Script Header
        </div>
        <div class="script">
          <vue-json-pretty :data="moduleScriptHeader"></vue-json-pretty>
        </div>
      </div>
        <div class="script-header">
          <div v-if="callingScript">
            Module Script
          </div>
          <div v-else>
            Script
          </div>
        </div>
        <div class="report-header">
            Report
        </div>
        <div class="script">
            <vue-json-pretty :data="script"></vue-json-pretty>
        </div>
        <div class="report">
            <vue-json-pretty :data="filteredReport"></vue-json-pretty>
        </div>
    </div>
  </div>
  </div>
</template>

<script>
    import VueJsonPretty from 'vue-json-pretty'
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
          //  return null;
          },
            filteredReport() {
              let clone = JSON.parse(JSON.stringify(this.report));
              if (clone.operation)
                clone.operation.extension = undefined;
              return clone;
            }
        },
        props: [
            // parts representing a single action
            'script', 'report', 'callingScript', 'moduleScript'
        ],
        components: {
            VueJsonPretty
        },
        name: "ScriptDisplay"
    }
</script>

<style scoped>
    .container {
        display: grid;
        grid-template-columns: 50% 50%;
    }
    .script-header {
        grid-column: 1;
        grid-row: 1;
        font-weight: bold;
    }
    .report-header {
        grid-column: 2;
        grid-row: 1;
        font-weight: bold;
    }
    .script {
        grid-column: 1;
        grid-row: 2;
    }
    .report {
        grid-column: 2;
        grid-row: 2;
    }

</style>
