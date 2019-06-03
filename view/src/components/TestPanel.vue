<template>
  <div>
    <!--route has simple ids-->
    <div v-if="variableSelectionValid">
      <div v-if="$route.params.variableId">
        <variable-edit :variable="thisVariable()"></variable-edit>
      </div>
      <div v-else>
        Test {{ $route.params.testId }}
      </div>
    </div>
  </div>
</template>
<script>

  import VariableEdit from "./VariableEdit";

  export default {
    data () {
      return {

      }
    },
    components: { VariableEdit },
    computed: {
      variableSelectionValid() {
        const variableId = this.$route.params.variableId
        const testId = this.$route.params.testId

        const testIndex = this.$store.state.base.tests.findIndex(function (test) {
          return test.id === testId
        })
        if (testIndex === -1) { return false }

        const variableIndex = this.$store.state.base.tests[testIndex].variables.findIndex( function (vari) {
          return vari.id === variableId
        })
        if (variableIndex === -1) { return false }
        return true
      }
    },
    methods: {
      thisVariable() {
        const variableId = this.$route.params.variableId
        const testId = this.$route.params.testId

        const testIndex = this.$store.state.base.tests.findIndex(function (test) {
          return test.id === testId
        })
        if (testIndex === -1) { throw `Cannot find test id ${testId} in TestPanel` }

        const variableIndex = this.$store.state.base.tests[testIndex].variables.findIndex( function (vari) {
          return vari.id === variableId
        })
        if (variableIndex === -1) { throw `Cannot find variable id ${variableId} in test ${testId} in TestPanel` }

        return this.$store.state.base.tests[testIndex].variables[variableIndex]
      }
    }
  }
</script>
<style scoped>
  .main {
    display: grid;
    grid-template-columns: 20% 80%;
    grid-template-areas: 'nav body';
  }
  .nav {
    text-align: left;
    align-content: start;
    grid-area: nav;
  }
  .body {
    text-align: left;
    align-content: start;
    grid-area: body;
  }

</style>
