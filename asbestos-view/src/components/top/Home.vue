<template>
    <div class="left">
        <h1>NIST FHIR Toolkit</h1>

        <h2>Self Test</h2>
        <div class="selectable" @click="selfTest()">Run</div>
        <div v-if="$store.state.log.loaded">
            Proxy responding
        </div>
        <div v-else>
            Proxy not responding
        </div>
        <div v-if="$store.state.testRunner.testCollectionsLoaded">
            Test Engine responding
        </div>
        <div v-else>
            Test Engine not responding
        </div>

        <h2>Major Components</h2>

        <h2>Controls</h2>
        The <span class="bold"> Control panel</span> is on the right side of the screen:<br /><br />

        <span class="bold">Test Session</span>
        - an area to work in giving isolation from other users.  Same as XDS Toolkit.
        <br />

        <span class="bold">Channel</span>
- a channel through the Proxy.  All traffic is routed through the Proxy which provides logs of the messages and translation. There are two
kinds of channels: FHIR - data passed without modification and MHD - translation is done between MHD and XDS formats.
        <br />

        <span class="bold">Events</span>
        - list the events for a chosen Channel.  Full details of an event can be displayed by selecting it.
        <br />

        <span class="bold">Test Collections</span>
        - There are two types of Test Collections: client and server.  Client tests are used to evaluate a
        SUT that initiates a transaction such as a Document Source. Server test evaluate SUTs that accept transactions such as a
        Document Recipient. Each Test Collection targets a particular actor. Once a Test Collection is selected it can be returned to
        by clicking on "View Selected".

        <h2>Patient Management</h2>
        All Document Sharing tests depend on a reference to a Patient resource. There is a Test Collection named Test Patients
        that can be used to load a small collection of Patient resources into a FHIR server. This must be done before any testing
        can be performed.  We suggest loading them into the default channel which points to the integrated HAPI FHIR server.
        This can be repeated without harm - the loading process checks and only loads what is needed.  There is no overlap between
        these Patient resources and the ones defined for Connectathon.

        To load:
        <ol>
            <li>Select the default channel</li>
            <li>Select Server Test Collections</li>
            <li>Select Test Patients from the Test Collections dropdown - the appropiate tests will display in the center</li>
            <li>Click Run All</li>
        </ol>

        <h2>Predefined Channels</h2>
        A collection of Channels comes pre-configured with the toolkit:<br /><br />

        <span class="bold">default</span>
        - leads to the integrated HAPI FHIR Server.  It performs no translation.  Logging only.
        <br /><br />

        <span class="bold">sut</span>
        - a placeholder for your System Under Test.

        <ul>
        <li>Use the Channel Editor (Edit in the Channels Control panel) to
            configure the FHIR Base Address before using.</li>
        </ul>
        <br />

        <span class="bold">xds</span>
        - leads to a Repository/Registry simulator in XDS Toolkit. Within the Channel Configuration, the XDS Site Name
        must be configured. On my system it is default__rr which is the default Test Session and the simulator rr.

        <ul>
        <li>This simulator must have <span class="bold">Validate Against Patient Identity Feed</span> unchecked as we do not
            send Patient Identity Feed messages to the simulator.</li>
        </ul>

        This Channel is used for validating MHD Comprehensive metadeata.
        <br /><br />

        <span class="bold">limited</span>
        - lead to a Repository/Registry simulator in XDS Toolkit.  Within the Channel Configuration, the XDS Site Name
        must be configured. On my system it is default__limited which is the default Test Session and the simulator limited.

        <ul>
        <li>This simulator must have <span class="bold">Validate Against Patient Identity Feed</span> unchecked as we do not
            send Patient Identity Feed messages to the simulator.</li>
            <li>This simulator must have <span class="bold">Metadata Limited</span> checked so validation is done using the
                rules for Metadata Limited messages.</li>
        </ul>


        This Channel is used for validating MHD Minimal metadeata.

    </div>
</template>

<script>
    export default {
        state() {
            return {

            }
        },
        methods: {
            selfTest() {
                this.$store.commit('resetLogLoaded')
                this.$store.commit('resetTestCollectionsLoaded')
                this.testTestEngine()
                this.testProxy()
            },
            testTestEngine() {
                this.$store.dispatch('loadTestCollectionNames')
            },
            testProxy() {
                this.$store.dispatch('loadEventSummaries')
            },
            testXdsToolkit() {

            },
            testHapi() {

            },
        },
        created() {
            this.selfTest()
        },
        name: "Home"
    }
</script>

<style scoped>

</style>
