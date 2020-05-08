<template>
    <div class="left">
        <h2>Setup</h2>
        <p>These are steps that must be completed when NIST FHIR Toolkit is installed. They can
        be re-run later to verify operation.</p>

        <h3>Load static resources</h3>
        <p>These resources are referenced in tests and must be loaded for tests to operate. They are loaded
            into the supporting HAPI server and to a cache in FHIR Toolkit located in the External Cache.
            This initialization can be re-run at any time.
            If the External Cache is cleared these must be run again to re-initialize the cache.
        </p>

        <!--  SelfTest is an alternate test runner   -->
        <self-test
            :session-id="'default'"
            :channel-id="'default'"
            :test-collection="'Test_Patients'"> </self-test>

        <h3>Self Tests</h3>
        <p>The following Test Collections are run against internal simulators to verify both the
        tests and the simulators. The status shows either success (green check) or failure/not run (red X).
            These can be re-run at any time. They depend on the above loading of static Resources.</p>

        <p>Run against MHD Document Recipient offering XDSonFHIR option - links to XDS Toolkit
            Repository/Registry simulator. </p>
        <self-test
                :session-id="'default'"
                :channel-id="'selftest_comprehensive'"
                :test-collection="'MHD_DocumentRecipient_comprehensive'"> </self-test>

        <p>Run against MHD Document Recipient offering XDSonFHIR option - links to XDS Toolkit Repository/Registry
            simulator configured to accept Limited Metadata. </p>
        <self-test
                :session-id="'default'"
                :channel-id="'selftest_limited'"
                :test-collection="'MHD_DocumentRecipient_minimal'"> </self-test>

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
