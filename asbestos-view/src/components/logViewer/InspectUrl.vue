<template>
  <div class="left left-margin panel">
    <log-analysis-report
      :the-url="dataObject"
    :no-inspect-label="true"> </log-analysis-report>
  </div>
</template>

<script>
import LogAnalysisReport from "./LogAnalysisReport";

export default {
  computed: {
    url() {
      return decodeURIComponent(this.dataObject)
    },
    path() {
      return decodeURIComponent(this.fhirPath)
    }
  },
  created() {
      console.log(`dataObject prop is ${this.dataObject}`)
      console.log(`fhirPath prop is ${this.fhirPath}`)
  },
  props: [
      'dataObject', 'fhirPath'
  ],
  components: {
      LogAnalysisReport
  },
  name: "InspectUrl"
}
</script>

<style scoped>
.left-margin {
  margin-left: 60px;
}
</style>
