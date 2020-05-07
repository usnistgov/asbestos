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

        <p>XDS Toolkit is only required for testing XDSonFHIR option.</p>

        <div class="selectable" @click="selfTest()">Run</div>
        <div v-if="$store.state.log.loaded">
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
            and follow the directions there.</p>

        <h2>FHIR Toolkit structure</h2>

        <p>FHIR Tookit uses four main components: the <span class="bold">Test Engine</span> sends messages and validates
        responses, the <span class="bold">Proxy</span> acts as an intermediary between components, logs messages, and performs
        translation between MHD and XDS formats, the <span class="bold">XDS Toolkit</span> validates XDS format messages
        once they have been transformed by the Proxy, and the <span class="bold">HAPI FHIR Server</span> is used to hold
        Patient resources.</p>

        <p>Details about how this structure is used to perform testing is discussed on the Configurations page (see top menu bar).</p>

        <h2>Controls</h2>
        The <span class="bold"> Control panel</span> is on the right side of the screen:<br /><br />

        <span class="bold">Test Session</span>
        - an area to work in giving isolation from other users.  Same as XDS Toolkit. Only one test session, default,
        is supported in this release.
        <br />

        <span class="bold">Channel</span>
- a channel through the Proxy.  All traffic is routed through the Proxy which provides logs of the messages and translation. There are two
kinds of channels: FHIR - data passed without modification and MHD - translation is done between MHD and XDS formats.
        <br />

        <span class="bold">Events</span>
        - list the events for a chosen Channel.  Full details of an event can be displayed by selecting it.
        <br />

        <span class="bold">Test Collections</span>
        - There are two types of Test Collections: client and server.  Client tests are used to evaluate an
        SUT that initiates a transaction such as a Document Source. Server tests evaluate SUTs that accept transactions such as a
        Document Recipient. Each Test Collection targets a particular actor.

        <h2>Patient Management</h2>
        All Document Sharing tests depend on a reference to a Patient resource. This toolit manages patients by
        loading a small set of Patient resources into the support HAPI server.  This procedure is handled in
        the Setup section above.

        <h2>Predefined Channels</h2>
        A collection of Channels comes pre-configured with the toolkit:<br /><br />

        <span class="bold">default</span>
        - leads to the integrated HAPI FHIR Server.  It performs no translation.  Logging only.
        <br /><br />

        <span class="bold">sut</span>
        - a placeholder for your System Under Test. This is used for server testing only.
        Use the Channel Editor (Config in the Channels Control panel) to
            configure the FHIR Base Address of your System Under Test before using.

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
        is configured. On my system it is default__limited which is the default Test Session and the simulator limited.

        This simulator must have <span class="bold">Validate Against Patient Identity Feed</span> unchecked as we do not
            send Patient Identity Feed messages to the simulator.
            This simulator must have <span class="bold">Metadata Limited</span> checked so validation is done using the
                rules for Metadata Limited messages.


        This Channel is used for validating MHD Minimal metadeata.

        <br /><br />
        <span class="bold">selftest_comprehensive</span>
            - leads to a Repository/Registry simulator in XDS Toolkit and is used for self tests on the Setup page.

        <br /><br />
        <span class="bold">selftest_minimal</span>
        - leads to a Repository/Registry simulator in XDS Toolkit and is used for self tests on the Setup page.

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
                this.$store.dispatch('loadEventSummaries',{session: 'default', channel: 'default'})
            },
            testXdsToolkit() {
                this.$store.dispatch('xdsHeartbeat')
            },
            testHapi() {
                this.$store.dispatch('hapiHeartbeat')
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
