<template>
    <div v-if="report">

        <!--    ERRORS    -->
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

        <div>All objects shown as retrieved from server</div>
        <div class="vdivider"></div>

        <!--  BASE OBJECT     -->
        <div>
            <span class="caption">Base Object:</span>
            <span v-if="report.source">{{ report.source }}</span>
            <div class="vdivider"></div>
            <div class="grid-container">
                <span v-if="report.base">
                    <div class="grid-item">
                        <span v-bind:class="objectDisplayClass(report.base)" @click="selectedResourceIndex = -1">
                            {{ report.base.name }}
                        </span>
                    </div>
                </span>
            </div>
        </div>

        <!--  RELATED     -->
        <div class="vdivider"></div>
        <span class="caption">Related:</span>
        <div class="vdivider"></div>
        <div class="grid-container">
            <span v-for="(resource, resourcei) in report.objects"
                :key="resource + resourcei">
                <div class="grid-item">
                    <span v-bind:class="objectDisplayClass(resource)" @click="selectedResourceIndex = resourcei">
                        {{ resource.name }} ({{ resource.relation }})
                    </span>
                </div>
            </span>
        </div>

        <!--  SELECTED      -->
        <div v-if="selectedResourceIndex === null"></div>
        <div v-else-if="selectedResourceIndex === -1">
            <div class="vdivider"></div>
            <div class="main-caption">{{ report.base.name }}</div>
            <div v-if="report.base.relation">
                {{ report.base.name }}
            </div>
            <div v-if="report.base.url">
                Ref: {{ report.base.url }}
            </div>
        </div>
        <div v-else-if="selectedResourceIndex > -1">
            <div class="vdivider"></div>
            <div class="main-caption">{{ report.objects[selectedResourceIndex].name }}</div>
            <div>
                <span class="caption">Relation:</span>
                {{ report.objects[selectedResourceIndex].relation }}</div>
            <div v-if="report.objects[selectedResourceIndex].url">
                <span class="caption">Ref:</span>
                {{ report.objects[selectedResourceIndex].url }}
            </div>
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
            loadAnalysis() {
                this.$store.dispatch('getLogEventAnalysis', {channel: this.channelId, session: this.sessionId, eventId: this.eventId})
                this.selectedResourceIndex = null
            },
            objectDisplayClass: function (resource) {
                return {
                    manifest: resource.name === 'DocumentManifest',
                    ref: resource.name === 'DocumentReference',
                    patient: resource.name === 'Patient'
                }
            },
        },
        computed: {
            report() {
                // content from gov.nist.asbestos.analysis.AnalysisReport.Report
                return this.$store.state.log.analysis
            },

        },
        created() {
            this.loadAnalysis()
        },
        watch: {
            'eventId': 'loadAnalysis'
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
.main-caption {
    font-weight: bold;
    font-size: larger;
}
</style>
