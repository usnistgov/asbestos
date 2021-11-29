<template>
  <div class="left">
    <div class="big-bold">
      <span>{{testCollection}}</span>
      <span>/{{testId}}</span>
      <span>/{{moduleId}}</span>
    </div>

    <div class="vdivider"> </div>
    <pre>{{testScriptAsString()}}</pre>
  </div>
</template>

<script>

export default {
  computed: {
    source() {
      if (this.moduleId) {
        return this.$store.state.testRunner.moduleTestScripts[`${this.testId}/${this.moduleId}`]
      } else {
        console.log(`loading simple testscript`)
        return this.$store.state.testRunner.testScripts[this.testId]
      }
    }
  },
  methods: {
    testScriptAsString() {
      return JSON.stringify( this.source, null, 2)
    },
    async loadTestScript() {
      if (!this.$store.state.testRunner.testScripts.hasOwnProperty(this.testId)) {
        //console.log(`testScript ${this.testId} needs loading`)
        if (this.$store.state.testRunner.currentTestCollectionName === null)
          this.$store.commit('setCurrentTestCollection', this.testCollection);
        await this.$store.dispatch('loadTestScripts', [this.testId]);
      }
    }
  },
  created() {
    //console.log(`DisplayTestSource`);
    this.loadTestScript();
  },
  props: [
      'testCollection', 'testId', 'scriptId', 'moduleId'
  ],
  components: {
  },
  name: "DisplayTestSource"
}
</script>

<style scoped>
</style>
