<template>
    <div>
        <div class="runallgroup">
            <span v-if="selfTestRunning" class="running">Running</span>
        </div>

        <div>

            <span>
                <span v-if="hasFailures">
                    <img src="../../assets/error.png" class="align-left">
                </span>
                <span v-else-if="hasNoRuns">
<!--                    <img src="../../assets/error.png" class="align-left">-->
                    <img src="../../assets/blank-circle.png" class="align-left">
                </span>
                <span v-else>
                    <img src="../../assets/checked.png" class="align-left">
                </span>
            </span>

            <span class="large-text">{{ cleanTestName(testCollection) }}</span>

            <span>
                <button class="runallbutton" @click.stop="runIt()">Run</button>
                    --  {{ earliestRunTime }}
            </span>

            <button class="runallbutton" @click.stop="doView(testCollection)">View</button>

            Channel {{ channelName }}
        </div>
    </div>
</template>

<script>
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import {ENGINE} from '../../common/http-common';
    import testCollectionMgmt from "../../mixins/testCollectionMgmt";
    import channelMixin from "@/mixins/channelMixin";

    export default {
        data() {
            return {
                selfTestRunning: false,
                hasFailures: false,
                allRun: false,
                earliestRunTime: null,
                hasNoRuns: true,
            }
        },
        methods: {
            async runIt() {
                this.selfTestRunning = true;
                await this.loadStatus('run');
                this.selfTestRunning = false;
            },
            async loadStatus(type) {
                const eventsForClientCollections =
                    'MHD_DocumentSource_comprehensive' === this.testCollection ? this.eventsForComprehensiveClientCollection:
                        'MHD_DocumentSource_minimal' === this.testCollection ? this.eventsForMinimalClientCollection: 30;
                const url = `selftest/${this.sessionId}__${this.channelName}/${this.testCollection}/${type}/${eventsForClientCollections}`;
                const promise = ENGINE.get(url);
                promise
                    .then(response => {
                        const data = response.data;
                        this.hasFailures = data.hasError;
                        this.allRun = data.allRun;
                        this.earliestRunTime = data.time;
                        this.hasNoRuns = data.noRuns;
                    })
                    .catch(function() {
                        this.$store.commit('setError', "Selftest failed - cannot reach server");
                    });
                await promise;
            },
            async doView() {
                const channelId = `${this.sessionId}__${this.channelName}`
                /*
                Assumes that the setup channel exists, either through the current test session config Includes or as a local channel.
                 */
                this.ftkLoadChannel(channelId, false, false). then(() => {
                    this.$router.push(`/session/${this.sessionId}/channel/${this.channelName}/collection/${this.testCollection}`)
                })
                // await this.loadTestCollection(this.testCollection);
                // this.$store.commit('setChannelName', this.channelName)
                // this.$router.push(`/session/${this.sessionId}/channel/${this.channelName}/collection/${this.testCollection}`)
            },
            async loadTestCollection(testCollection) {
                this.$store.commit('setCurrentTestCollection', testCollection);
                await this.$store.dispatch('loadCurrentTestCollection')
            },
        },
        created() {
            if (this.autoLoad)
                this.loadStatus("status");
            //this.channel = this.channelId;
        },
        mixins: [ colorizeTestReports, testCollectionMgmt, channelMixin ],
        name: "SelfTest",
        props: [
            'sessionId',
          'channelName',   // simple name (really channelName)
          'testCollection',
          'autoLoad'
        ],
        components: {
        }
    }
</script>

<style scoped>
    .runallgroup {
        text-align: center;
        padding-bottom: 5px;
    }

</style>
