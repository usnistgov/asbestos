<template>
  <div>
    <h2 class="conformance-tests-header">Conformance Tests</h2>
    <div class="tool-title">
      Test Collection:
      <span>{{ cleanTestName(testCollection) }}</span>
      <span class="divider"></span>
      <img id="reload" class="selectable" @click="loadTestCollection(testCollection)" src="../../assets/reload.png"/>
      <span class="divider"></span>
    </div>

    <div class="vdivider"></div>
    <div class="vdivider"></div>

    <div class="left">
      Description: <span v-html="testCollectionDescription"></span>
    </div>

    <div class="vdivider"></div>
    <div class="vdivider"></div>

    <div class="instruction">
            <span v-if="$store.state.testRunner.isClientTest"  class="instruction">
                These are Client tests - the system under test sends messages to the
                FHIR Server Base Address shown below for evaluation. To run:
                <br />
                <ol>
                    <li>Send messages matching each per-test description.</li>
                    <li>Response message will reflect evaluation by the FHIR server (or XDS server for MHD tests) running in the background.</li>
<!--                    <li>Once an adequate collection of messages has been sent to satisfy the tests, evaluate them further by clicking the-->
<!--                    spyglass icon to evaluate against a set of assertions specific to each test.-->
<!--                    This evaluation will include validating the response from the background server.</li>-->
                    <li>Only the most recent messages will be evaluated.
                        Adjust the count below *.
                        <template v-if="testCollection.endsWith('_DocumentSource_minimal')">
                        If evaluating minimal metadata test collection run against an Asbestos self-test channel, use {{eventsForMinimalClientCollection}} most recent events.
                        </template>
                        <template v-if="testCollection.endsWith('_DocumentSource_comprehensive')">
                        If evaluating comprehensive metadata tests run against an Asbestos self-test channel, use {{eventsForComprehensiveClientCollection}} most recent events.
                        </template>
                        </li>
                    <li>A test passes if one or more message evaluates correctly.</li>
                    <li>Click on a test to see the messages evaluated.  Click on a message to see the result of each
                        assertion that was evaluated.</li>
                </ol>
                <div>
                    Send to:
                    <span class="boxed">{{ clientBaseAddress }}</span>  based on the Channel selection.
                </div>
            </span>
      <span v-else  class="instruction">
                These are server tests
        <!-- Display the property, if it exists, based on channel Type -->
                <div v-if="theChannelObj">
                    <span v-if="theChannelObj.channelType === 'passthrough' || theChannelObj.channelType === 'fhir'">
                        Requests will be sent to
                        <span v-if="theChannelObj.fhirBase" class="boxed">{{ theChannelObj.fhirBase }}</span>
                        <div class="divider"></div>
                        (through the Proxy on Channel {{ theChannelObj.channelName }}) based on the Channel selection.
                    </span>
                    <span v-else-if="theChannelObj.channelType === 'mhd'">
                         Requests will be sent to XDS Site:
                        <span v-if="theChannelObj.xdsSiteName" class="boxed">{{ theChannelObj.xdsSiteName }}</span>
                        <div class="divider"></div>
                        (through the Proxy on MHD Channel {{ theChannelObj.channelName }}) based on the Channel selection.
                    </span>
                    <span v-else class="configurationError">
                        Unknown channel.channelType for {{theChannelObj.channelName }}.
                    </span>
                </div>
            </span>
      <span class="divider"></span>
    </div>

    <div v-if="$store.state.testRunner.isClientTest" class="second-instruction">
      * Number of most recent events to evaluate:
      <input v-model="evalCount" placeholder="5">
        <p>Tests are run automatically when this page is loaded.</p>
    </div>


  </div>
</template>

<script>
import testCollectionMgmt from "../../mixins/testCollectionMgmt";

export default {
  methods: {
    load() {
      // this.loadTestCollection(this.testCollection)
    },
  },
  computed: {
    testCollectionDescription() {
        return this.$store.state.testRunner.collectionDescription
    }
  },
  created() {
    // this.load(this.testCollection)
    // this.setEvalCount()
  },
  mounted() {
        this.$store.subscribe((mutation) => {
            if (mutation.type === 'ftkChannelLoaded') {
                if (this.$store.state.base.ftkChannelLoaded) {
                    // console.log('TestCollectionHeader syncing on mutation.type: ' + mutation.type)
                    this.load(this.testCollection)
                    this.setEvalCount()
                }
            }
        })
    },
    watch: {
    'evalCount': 'setEvalCount',
    'testCollection': 'load',
  },
  mixins: [ testCollectionMgmt ],
  name: "TestCollectionHeader",
  props: [
    'sessionId', 'channelName', 'testCollection',
  ],
}
</script>

<style scoped>

</style>
