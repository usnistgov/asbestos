<template>
    <div class="left">

        <h2>Services</h2>
        <p>Verify that all back-end services are responding.</p>

        <p>For problems with Proxy or Test Engine status look in
            &lt;asbestos>/tomcat/Toolkits/FhirToolkit/webapps/ROOT/serviceProperties.json.
        Property fhirToolkitBase is likely the problem.
            Changes to serviceProperties.json require toolkit restart (Tomcat).</p>

        <p>For problems with HAPI and XDS look in
            &lt;asbestos>/tomcat/Toolkits/FhirToolkit/conf/service.properties.  Link to HAPI is hapiFhirBase.
            Link to XDS is xdsToolkitBase. Changes to service.properties require toolkit restart (Tomcat).</p>

        <p>XDS Toolkit is required for testing XDSonFHIR option and for running self tests in Setup.</p>

        <div class="selectable" @click="selfTest()"><img src="../../assets/reload.png"/>&nbsp;Refresh</div>
        <div v-if="isProxyResponding">
            <img src="../../assets/check.png">
            Proxy is responding at {{proxyBase()}}
        </div>
        <div v-else>
            <img src="../../assets/cross.png">
            Proxy is <b>not</b> responding at {{proxyBase()}}
        </div>
        <div v-if="$store.state.testRunner.testCollectionsLoaded">
            <img src="../../assets/check.png">
            Test Engine is responding at {{testEngineBase()}}
        </div>
        <div v-else>
            <img src="../../assets/cross.png">
            Test Engine is <b>not</b> responding at {{testEngineBase()}}
        </div>
        <div v-if="$store.state.heartbeat.hapiIsAlive">
            <img src="../../assets/check.png">
            HAPI server is responding at {{$store.state.heartbeat.hapiDetails}}
        </div>
        <div v-else>
            <img src="../../assets/cross.png">
            HAPI server is <b>not</b> responding at {{$store.state.heartbeat.hapiDetails}}
        </div>
        <div v-if="$store.state.heartbeat.xdsIsAlive">
            <img src="../../assets/check.png">
            XDS Toolkit is responding at {{$store.state.heartbeat.xdsDetails}}
        </div>
        <div v-else>
            <img src="../../assets/cross.png">
            XDS Toolkit is <b>not</b> responding at {{$store.state.heartbeat.xdsDetails}}
        </div>

        <h2>Setup</h2>
        <p>When this toolkit is first installed it must be initialized.  Go to the Setup page (top menu ribbon)
            and follow the directions there. All above listed services must show green checks
        before Setup can be usefully run.</p>

        <h2>FHIR Toolkit structure</h2>

        <p>FHIR Tookit uses four main components: the <span class="bold">Test Engine</span> sends messages and validates
        responses, the <span class="bold">Proxy</span> (a recording proxy) acts as an intermediary between components,
            logs messages, and performs
        translation between MHD and XDS formats, the <span class="bold">XDS Toolkit</span> validates XDS format messages
        once they have been transformed by the Proxy, and the <span class="bold">HAPI FHIR Server</span> is used to hold
        Patient resources by default.</p>

        <p>Details about how this structure is used to perform testing is discussed on the Configurations page (see top menu bar).</p>

        <h2>Controls</h2>
        The <span class="bold"> Control panel</span> is on the right side of the screen:<br /><br />

        <span class="bold">Test Session</span>
        - an area to work in giving isolation from other users. Similar to XDS Toolkit. To prevent accidental deletion of the test
        session contents, the session configuration must be locked. For now, this will be a manual step on the server.
        Edit the <span class="fixedWidthFont">ExternalCache\FhirSessions\theTestSession\config.json</span> file and add
        the <span class="fixedWidthFont">"sessionConfigLocked":true</span> JSON property. Two other properties become relevant
        if session config is locked: <span class="fixedWidthFont">canAddChannel</span> and <span class="fixedWidthFont">canRemoveChannel</span>.
        They offer whether the channel level add/remove features are enabled to the user.
        <br />

        <span class="bold">Channel</span>
- a channel through the Proxy.  All traffic is routed through the Proxy which provides logs of the messages and translation. There are two
kinds of channels: FHIR - data passed without modification and MHD - translation is done between MHD and XDS formats. By naming convention, a channel Id is
        displayed with a test session prefix in the Channel control panel if it originates from an Included test session. Channel name part without the test session
        prefix is displayed for channels local the current test session.
        To prevent accidental configuration changes, channels can be locked by the Admin SignIn feature.
        A lock icon appears in the detailed channel configuration area if the channel configuration is locked.
        <br />

        <span class="bold">Events</span>
        - list the events for a chosen Channel.  Each message through the Proxy generates an event. Full details of
        an event can be displayed by selecting it.
        <br />

        <span class="bold">Test Collections</span>
        - All tests reside in a Test Collection. There are two types of Test Collections: client and server.  Client tests are used to evaluate an
        SUT that initiates a transaction such as a Document Source. Server tests evaluate SUTs that accept transactions such as a
        Document Recipient. Each Test Collection targets a particular actor.

        <h2>Patient Management</h2>
        Document Sharing tests may depend on a reference to a Patient resource. This toolkit manages patients by
        loading a small set of Patient resources into the support HAPI server (by default).  This procedure is handled in
        the Setup section above.

        <h2>Predefined Channels</h2>
        A collection of Channels comes pre-configured with the toolkit:<br /><br />

        <span class="bold">default</span>
        - leads to the integrated HAPI FHIR Server.  It performs no translation.  Logging only.
        <br /><br />

        <span class="bold">external_patient</span>
        - If the <span class="fixedWidthFont">patientServerBase</span> setting in Service Properties is enabled with a proper FHIR base, then it leads to the external FHIR Server used for Connectathon patient reference testing purposes. Conformance Tests will use the external reference instead of the integrated patient reference. This is not used for local testing purpose. Channel performs no translation.  Logging only
        <br /><br />

        <span class="bold">sut</span>
        - a placeholder for your System Under Test. This is used for server testing only.
        Use the Channel Editor (Config in the Channels Control panel) to
            configure the FHIR Base Address of your System Under Test before using.
        <br /><br />

        <span class="bold">xds</span>
        - leads to a Repository/Registry simulator in XDS Toolkit. The Channel Configuration contains the XDS Site Name.
        This is configured at system start up to point to the default__asbtsrr simulator in XDS Toolkit. The location of
        XDS Toolkit is identified in the Service Properties file.

        This simulator must have <span class="bold">Validate Against Patient Identity Feed</span> unchecked as we do not
            send Patient Identity Feed messages to the simulator.

        This Channel is used for validating MHD Comprehensive metadeata.
        <br /><br />

        <span class="bold">limited</span>
        - leads to a Repository/Registry simulator in XDS Toolkit.  Within the Channel Configuration, the XDS Site Name
        is configured.

        This simulator must have <span class="bold">Validate Against Patient Identity Feed</span> unchecked as we do not
            send Patient Identity Feed messages to the simulator.
            This simulator must have <span class="bold">Metadata Limited</span> checked so validation is done using the
                rules for Metadata Limited messages.

        DocumentReference.subject and DocumentManifest.subject are optional in Minimal Metadata. For the translation
        to XDS, a patient named No Patient is inserted to create valid XDS metadata.  This reference is removed on
        queries so it is transparent to FHIR resource processing.

        This Channel is used for validating MHD Minimal metadata.

        <br /><br />
        <span class="bold">selftest_comprehensive</span>
            - leads to a Repository/Registry simulator in XDS Toolkit and is used for self tests on the Setup page.

        <br /><br />
        <span class="bold">selftest_minimal</span>
        - leads to a Repository/Registry simulator in XDS Toolkit and is used for self tests on the Setup page.
        <br /><br />

        Only <span class="bold">sut</span> is intended to be edited by the user.  Altering other pre-installed channels
        may disable some processing within toolkit. New channels can always be added.

        <h2>FHIR Toolkit Configuration</h2>
        For details about configuration look in the <a href="https://github.com/usnistgov/asbestos/wiki/Configuration" target="_blank">wiki</a>.

    </div>
</template>

<script>
    import {initServiceProperties, UtilFunctions} from "../../common/http-common";

    export default {
        state() {
            return {

            }
        },
        methods: {
            selfTest() {
                this.$store.commit('resetLogLoaded')
                this.$store.commit('resetTestCollectionsLoaded')
                this.$store.commit('setHapiIsAlive', false)
                this.$store.commit('setXdsIsAlive', false)
                initServiceProperties()
                this.testTestEngine()
                this.testProxy()
                this.testHapi()
                this.testXdsToolkit()
                this.setChannelTypeIgTestCollections()
            },
            testEngineBase() {
                return UtilFunctions.getTestEngineBase()
            },
            proxyBase() {
                return UtilFunctions.getProxyBase()
            },
            testTestEngine() {
                this.$store.dispatch('loadTestCollectionNames')
            },
            testProxy() {
                this.$store.dispatch('loadEventSummaries',{testSession: 'default', channel: 'default', itemsPerPage: 1, page: 1})
            },
            testXdsToolkit() {
                this.$store.dispatch('xdsHeartbeat')
            },
            testHapi() {
                this.$store.dispatch('hapiHeartbeat')
            },
            setChannelTypeIgTestCollections() {
                this.$store.dispatch('loadChannelTypeIgTestCollections')
            }
        },
        computed: {
            isProxyResponding() {
                return this.$store.getters.isProxyResponding
            }
        },
        created() {
            this.selfTest()
        },
        name: "Home"
    }
</script>

<style scoped>
    .fixedWidthFont {
        font-family: monospace;
    }

</style>
