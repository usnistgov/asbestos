<template>
    <div v-if="report">
        <div class="vdivider"></div>
        <div v-if="report.errors.length > 0">
            <span class="caption">Errors:</span>
            <div class="vdivider"></div>
            <div v-for="(err, erri) in report.errors"
                :key="err + erri">
                <div>
                    {{ report.errors[erri]}}
                </div>
            </div>
        </div>
        <div class="vdivider"></div>
        <span class="caption">Contents:</span>
        <span v-if="report.source">{{ report.source }}</span>
        <div class="vdivider"></div>
        <div class="grid-container">
            <span v-for="(resource, resourcei) in report.objects"
                :key="resource + resourcei">
                <div class="grid-item">
                    <span v-bind:class="{
                        manifest: resource === 'DocumentManifest',
                        ref: resource === 'DocumentReference',
                        patient: resource === 'Patient'
                    }
                    " @click="selectedResourceIndex = resourcei">
                        {{ resource }}
                    </span>
                </div>
            </span>
        </div>
        <div v-if="selectedResourceIndex >= 0">
            <div class="vdivider"></div>
            <div class="caption">{{ report.objects[selectedResourceIndex] }}</div>
        </div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                selectedResourceIndex: null,
            }
        },
        methods: {

        },
        computed: {
            report() {
                // content from gov.nist.asbestos.analysis.AnalysisReport.Report
                return this.$store.state.log.analysis
            },
        },
        created() {
            this.$store.dispatch('getLogEventAnalysis', {channel: this.channelId, session: this.sessionId, eventId: this.eventId})
        },
        props: [
            'sessionId', 'channelId', 'eventId'
        ],
        name: "LogAnalysisReport"
    }
</script>

<style scoped>
.button {
    /*border: 1px solid black;*/
    cursor: pointer;
    /*padding: 5px;*/
}
.grid-container {
    display: flex;
    grid-template-columns: auto auto auto;
    /*background-color: #2196F3;*/
    /*padding: 10px;*/
}
.grid-item {
    /*background-color: rgba(255, 255, 255, 0.8);*/
    border: 1px solid rgba(0, 0, 0, 0.8);
    text-align: center;
}
    .manifest {
        background-color: #0086B3;
        border: 1px solid rgba(0, 0, 0, 0.8);
        text-align: center;
        cursor: pointer;
        padding: 5px;
    }
    .ref {
        background-color: coral;
        border: 1px solid rgba(0, 0, 0, 0.8);
        text-align: center;
        cursor: pointer;
        padding: 5px;
    }
    .patient {
        background-color: #999988;
        border: 1px solid rgba(0, 0, 0, 0.8);
        text-align: center;
        cursor: pointer;
        padding: 5px;
    }
.vdivider{
    height:6px;
    width:auto;
}
    .caption {
        font-weight: bold;
    }
</style>
