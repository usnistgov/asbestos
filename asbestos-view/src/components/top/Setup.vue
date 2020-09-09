<template>
    <div class="left">
        <h2>Setup</h2>
        <p>These are steps that must be completed when NIST FHIR Toolkit is installed. They can
        be re-run later to verify operation.</p>

        <h3>Build static resources</h3>
        <p>These resources are referenced in tests and must be loaded for tests to operate. They are loaded
            into the supporting HAPI server (default channel) and to a cache in FHIR Toolkit located in the External Cache.
            This initialization can be re-run at any time.
            If the External Cache is cleared these must be run again to re-initialize the individual caches.
            Once the individual caches are established for the default channel it is available for testing on any channel.

            To store the static resources in a different server/channel, manually select the channel and the
            test collection (both on the right side of the window) and use RunAll button to load.
            This channel-specific cache will only be used for this channel.
            The cache on the default channel is always available for when there is no channel-specific cache
            available. Said another way, the channel-specific cache (the one you chose) is checked first.  Then the
            default channel cache is checked second.
        </p>

      <h3>Static resource lookup</h3>
      <p>Static fixtures are defined with &lt;fixture>&lt;/fixture> declaration in a TestScript.
        Inside this declaration is a reference to the content:</p>
      <pre>
    &lt;resource>
       &lt;reference value="Bundle/binary_bundle.xml"/>
    &lt;/resource>
      </pre>
      The search path for this content is:
        <ol>
      <li>The test definition (sub-directory Bundle, file binary_bundle.xml)</li>
      <li>Resource cache for the current channel (EXTERNAL_CACHE/FhirTestLogs/default__CHANNEL/cache : sub-directory Bundle, file binary_bundle.xml)</li>
      <li>Resource cache for the default channel (EXTERNAL_CACHE/FhirTestLogs/default__default/cache : sub-directory Bundle, file binary_bundle.xml)</li>
    </ol>


        <!--  SelfTest is an alternate test runner   -->
        <self-test
            :session-id="'default'"
            :channel-id="'default'"
            :auto-load="true"
            :test-collection="'Test_Patients'"> </self-test>
      <self-test
          :session-id="'default'"
          :channel-id="'default'"
          :auto-load="true"
          :test-collection="'Test_Documents'"> </self-test>

        <h3>Self Tests</h3>
        <p>The following Test Collections are run against internal simulators to verify both the
        tests and the simulators. The status shows either success (green check) or failure/not run (red X).
            These can be re-run at any time. They depend on the above loading of static Resources.</p>

        <p>Note: these statuses do not refresh when the page reloads. Every time you navigate to this
        page the status will show error.  Running the test will update the status.</p>


        <h4>These two tests must be run one after another.</h4>
        <p>Run against MHD Document Recipient offering XDSonFHIR option - links to XDS Toolkit
            Repository/Registry simulator. This also loads events into the channel logs that the Document Source
        self test (below) depends on. If other work is done in between then the Document Source test will
        likely fail. This is because the Document Source tests only look so far back in the history (created by this
            test) of the
        channel.</p>
        <self-test
                :session-id="'default'"
                :channel-id="'selftest_comprehensive'"
                :test-collection="'MHD_DocumentRecipient_comprehensive'"> </self-test>

        <p>Run against MHD Document Source using XDSonFHIR option - links to XDS Toolkit
            Repository/Registry simulator for evaluation. </p>
        <self-test
                :session-id="'default'"
                :channel-id="'selftest_comprehensive'"
                :test-collection="'MHD_DocumentSource_comprehensive'"> </self-test>

        <h4>These two tests must be run one after another</h4>
        <p>Run against MHD Document Recipient - links to XDS Toolkit Repository/Registry
            simulator configured to accept Limited Metadata.
            This also loads events into the channel logs that the Document Source
            self test (below) depends on. If other work is done in between then the Document Source test will
            likely fail. This is because the Document Source tests only look so far back in the history
            (created by this
            test) of the
            channel.</p>
        <self-test
                :session-id="'default'"
                :channel-id="'selftest_limited'"
                :test-collection="'MHD_DocumentRecipient_minimal'"> </self-test>

        <p>Run against MHD Document Source - links to XDS Toolkit Repository/Registry
            simulator configured to accept Limited Metadata for evaluation.
            </p>
        <self-test
                :session-id="'default'"
                :channel-id="'selftest_limited'"
                :test-collection="'MHD_DocumentSource_minimal'"> </self-test>
    </div>
</template>

<script>
    import testCollectionMgmt from "../../mixins/testCollectionMgmt";
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    //import SelfTestInstalls from "../testRunner/SelfTestInstalls";
    import SelfTest from "../testRunner/SelfTest";
//    import {CHANNEL} from "../../common/http-common";

    export default {
        methods: {
        },
        computed: {
        },
        created() {
        },
        mixins: [ testCollectionMgmt, colorizeTestReports ],
        name: "Setup",
        props: [

        ],
        components: {
            //SelfTestInstalls,
            SelfTest
        }

    }
</script>

<style scoped>

</style>
