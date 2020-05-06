<template>
    <div class="left">
        <h2>Setup</h2>
        <p>These are steps that must be completed when NIST FHIR Toolkit is installed. They can
        be re-run later to verify operation.</p>

        <h3>Load static resources</h3>
        <p>These resources are referenced in tests and must be loaded for tests to operate. They are loaded
            into the supporting HAPI server. This initialization can be re-run at any time.
        </p>

        <!--  SelfTest is an alternate test runner   -->
        <self-test
            :session-id="'default'"
            :channel-id="'default'"
            :test-collection="'Test_Patients'"> </self-test>

        <h3>Self Tests</h3>
        <p>The following Test Collections are run against internal simulators to verify both the
        tests and the simulators. The status shows either success (green check) or failure/not run (
        red X). These can be re-run at any time.</p>

        <p>Run against XDS Toolkit Repository/Registry simulator. </p>
        <self-test
                :session-id="'default'"
                :channel-id="'selftest_comprehensive'"
                :test-collection="'MHD_DocumentRecipient_Comprehensive'"> </self-test>

        <p>Run against XDS Toolkit Repository/Registry simulator that accepts Limited Metadata. </p>
        <self-test
                :session-id="'default'"
                :channel-id="'selftest_limited'"
                :test-collection="'MHD_DocumentRecipient_Minimal'"> </self-test>

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
