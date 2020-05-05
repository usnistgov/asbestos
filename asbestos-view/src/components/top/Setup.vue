<template>
    <div class="left">
        <h2>Setup</h2>
        <p>These are steps that must be completed when NIST FHIR Toolkit is installed. They can
        be re-run later to verify operation.</p>

        <h3>Load static Patient resources</h3>
        <p>These Patient resources are referenced in tests and must be loaded for tests to operate.
        </p>

        <!--  SelfTest is an alternate test runner   -->
        <self-test-installs
            :session-id="'default'"
            :channel-id="'default'"
            :test-collection="'Test_Patients'"> </self-test-installs>

        <h3>Self Tests</h3>
        <p>The following Test Collections are run against internal simulators to verify both the
        tests and the simulators.</p>

<!--        <self-test-->
<!--                :session-id="'default'"-->
<!--                :channel-id="'selftest_comprehensive'"-->
<!--                :test-collection="'Modular_Comprehensive'"> </self-test>-->

    </div>
</template>

<script>
    import testCollectionMgmt from "../../mixins/testCollectionMgmt";
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import SelfTestInstalls from "../testRunner/SelfTestInstalls";
    import {CHANNEL} from "../../common/http-common";

    export default {
        methods: {
            async loadPatients() {
                // this will force change to channel default - configured in test collection
                this.$store.commit('setCurrentTestCollection', 'Test_Patients')
                await this.$store.dispatch('loadCurrentTestCollection')

                await this.$store.dispatch('loadTestScripts', this.$store.state.testRunner.testScriptNames)
                await this.$store.dispatch('loadTestReports', this.$store.state.testRunner.currentTestCollectionName)
            },
            async createChannel(channel) {
                await CHANNEL.post('', channel)
                     .catch(function (error) {
                          this.$store.state.testRunner.commit('setError',
                              `Error creating channel ${channel.channelId}` + ': ' + error)
                     })
            },
            async createChannels() {
                let reload = false;
                if (!this.$store.getters.channelExists('selftest_comprehensive')) {
                      const channel = {};
                      channel.fhirBase = '';
                      channel.actorType = 'fhir';
                      channel.xdsSiteName = 'default__asbtsrr';
                      channel.testSession = 'default';
                      channel.channelId = 'selftest_comprehensive';
                      channel.channelType = 'mhd';
                      channel.environment = 'default';
                      channel.writeLocked = false;
                      await CHANNEL.post('', channel);
                      reload = true;
                }
                if (!this.$store.getters.channelExists('selftest_default')) {
                    await this.$store.dispatch('loadHapiFhirBase');
                    const channel = {};
                    channel.fhirBase = this.$store.state.testRunner.hapiFhirBase;
                    channel.actorType = 'fhir';
                    channel.xdsSiteName = '';
                    channel.testSession = 'default';
                    channel.channelId = 'selftest_default';
                    channel.channelType = 'fhir';
                    channel.environment = 'default';
                    channel.writeLocked = false;
                    await CHANNEL.post('', channel);
                    reload = true;
                }
                if (reload)
                    await this.$store.dispatch('loadChannelNames')
            },
        },
        computed: {
            isPatientsLoaded() {
                return false;
            },
            isPatientsSuccess() {
                return false;
            },
        },
        created() {
            //this.createChannels();
            this.loadPatients();
        },
        mixins: [ testCollectionMgmt, colorizeTestReports ],
        name: "Setup",
        props: [

        ],
        components: {
            SelfTestInstalls
        }

    }
</script>

<style scoped>

</style>
