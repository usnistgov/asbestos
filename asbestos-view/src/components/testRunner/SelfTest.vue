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
                    <img src="../../assets/error.png" class="align-left">
<!--                    <img src="../../assets/blank-circle.png" class="align-left">-->
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

            Channel {{ channelId }}
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
                allRun: false,
                earliestRunTime: null,
                hasNoRuns: true,
                eventsForClientCollections: 30,
            }
        },
        methods: {
            async runIt() {
                this.running = true;
                await this.loadStatus('run');
                this.running = false;
            },
            async loadStatus(type) {
                const url = `selftest/${this.sessionId}__${this.channelId}/${this.testCollection}/${type}/${this.eventsForClientCollections}`;
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
                await this.loadTestCollection(this.testCollection);
                this.$store.commit('setChannelId', this.channelId)
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`)
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
        mixins: [ colorizeTestReports, testCollectionMgmt ],
        name: "SelfTest",
        props: [
            'sessionId', 'channelId', 'testCollection', 'autoLoad'
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
