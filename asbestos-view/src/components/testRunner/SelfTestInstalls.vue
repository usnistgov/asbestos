<template>
    <div>
        <div class="runallgroup">
            <span v-if="running" class="running">Running</span>
        </div>

        <div>

            <span>
                <span v-if="hasFailures">
                    <img src="../../assets/error.png" class="align-left">
                </span>
                <span v-else-if="hasNoRuns">
                    <img src="../../assets/blank-circle.png" class="align-left">
                </span>
                <span v-else>
                    <img src="../../assets/checked.png" class="align-left">
                </span>
            </span>

            <span class="large-text">{{ cleanTestName(testCollection) }}</span>

            <span>
                <button class="runallbutton" @click.stop="doRunAll()">Run</button>
                    --  {{ collectionTime }}
            </span>

            <button class="runallbutton" @click.stop="doView(testCollection)">View</button>

            Uses channel {{ channelId }}
        </div>
    </div>
</template>

<script>
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import {ENGINE} from '../../common/http-common';
    import testCollectionMgmt from "../../mixins/testCollectionMgmt";

    export default {
        data() {
            return {
                hasFailures: false,
                hasNoRuns: false,
                earliestRunTime: null,
                hasError: null,
                allRun: false,
            }
        },
        methods: {
            async doView(testCollection) {
                await this.loadTestCollection(testCollection);
                this.$store.commit('setChannelId', this.channelId)
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/collection/${testCollection}`)
            },
            async loadStatus() {
                console.log(`Installs starts`)
                const url = `selftest/${this.sessionId}__${this.channelId}/${this.testCollection}/status`;
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
                console.log(`Installs ends`)
            },
            // This is close to a duplicate of testRunner:loadTestReports except
            // here the state is maintained in the component.  The
            // store is organized for loading one collection at at time.
            // This is supporting a multiple collection survey
            async loadTestReports(sessionId, channelId, collectionId) {
                console.log(`Installs starts`)

                const testIds = this.$store.state.testRunner.testScriptNames;
                const promises = [];
                let hasFailures = false;
                let hasNoRuns = false;
                let earliestRunTime = null;
                testIds.forEach(testId => {
                    const url = `testReport/${sessionId}__${channelId}/${collectionId}/${testId}`
                    const promise = ENGINE.get(url)
                    promises.push(promise)
                })
                const combinedPromises = Promise.all(promises)
                    .then(results => {
                        results.forEach(result => {
                            const testReport = result.data
                            if (testReport) {
                                if (testReport.result === 'fail')
                                    hasFailures = true;
                                if (testReport.issued && !earliestRunTime)
                                    earliestRunTime = testReport.issued;
                                else if (testReport.issued && testReport.issued < earliestRunTime)
                                    earliestRunTime = testReport.issued;
                            } else {
                                hasNoRuns = true;
                            }
                        })
                    })
                    .catch(function(error) {
                        this.state.errors.push(`Loading reports: ${error}`)
                    })
                await combinedPromises
                this.hasFailures = hasFailures;
                this.hasNoRuns = hasNoRuns;
                this.earliestRunTime = earliestRunTime;
                console.log(`Installs ends`)

            },
            async loadTestCollection(testCollection) {
                this.$store.commit('setCurrentTestCollection', testCollection);
                await this.$store.dispatch('loadCurrentTestCollection')
            },
        },
        created() {
            this.loadStatus();
            //this.loadTestReports(this.sessionId, this.channelId, this.testCollection);
        },
        mixins: [ colorizeTestReports, testCollectionMgmt ],
        name: "SelfTestInstalls",
        props: [
            'sessionId', 'channelId', 'testCollection',
        ],
        components: {
        }
    }
</script>

<style scoped>

</style>
