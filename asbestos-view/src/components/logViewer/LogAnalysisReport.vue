<template>
    <div v-if="report" class="boxed">

        <!--    ERRORS    -->
        <div class="vdivider"></div>
        <div v-if="report.errors.length > 0 && report.errors[0] !== null">  <!--  don't know why the null check is needed but it is  -->
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

        <div>Resource IDs are extracted from the Response message. Content shown comes directly from the server
            via separate retrieves.</div>
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
        <div class="vdivider"></div>

        <!--  SELECTED      -->
        <div v-if="selectedResourceIndex === null"></div>

        <!--  BASE OBJECT DETAILS -->
        <div v-else-if="selectedResourceIndex === -1">
            <div class="vdivider"></div>
            <div class="main-caption">{{ report.base.name }}</div>
            <div v-if="report.base.relation">
                {{ report.base.name }}
            </div>
            <div v-if="report.base.url">
                <span class="caption"> Ref:</span>
                {{ report.base.url }}
            </div>

            <div>
                <span>Comprehensive Metadata </span>
                <span v-if="report.base.isComprehensive"><img src="../../assets/check.png"></span>
                <span v-else><img src="../../assets/cross.png"></span>
                <div class="divider"></div>
                <log-error-list :errorList="report.base.comprehensiveErrors" :attList="report.base.comprehensiveChecked" :att-list-name="'Required Attributes'"> </log-error-list>
            </div>
            <div>
                <span>Minimal Metadata </span>
                <span v-if="report.base.isMinimal"><img src="../../assets/check.png"></span>
                <span v-else><img src="../../assets/cross.png"></span>
                <div class="divider"></div>
                <log-error-list :errorList="report.base.minimalErrors" :attList="report.base.minimalChecked" :att-list-name="'Required Attributes'"> </log-error-list>
            </div>
            <div>
                <span class="caption">Coding</span>
                <span v-if="report.base.codingErrors.length === 0"><img src="../../assets/check.png"></span>
                <span v-else><img src="../../assets/cross.png"></span>
                <log-error-list :errorList="report.base.codingErrors"> </log-error-list>
            </div>
        </div>

        <!--  RELATED OBJECT DETAILS -->
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
            <div v-if="report.objects[selectedResourceIndex].name === 'DocumentManifest' || report.objects[selectedResourceIndex].name === 'DocumentReference'">
                <div>
                    <span class="caption">Comprehensive Metadata </span>
                    <span v-if="report.objects[selectedResourceIndex].isComprehensive"><img src="../../assets/check.png"></span>
                    <span v-else><img src="../../assets/cross.png"></span>
                    <div class="divider"></div>
                    <log-error-list :errorList="report.objects[selectedResourceIndex].comprehensiveErrors" :attList="report.objects[selectedResourceIndex].comprehensiveChecked" :att-list-name="'Required Attributes'"> </log-error-list>
                </div>
                <div>
                    <span class="caption">Minimal Metadata </span>
                    <span v-if="report.objects[selectedResourceIndex].isMinimal"><img src="../../assets/check.png"></span>
                    <span v-else><img src="../../assets/cross.png"></span>
                    <div class="divider"></div>
                    <log-error-list :errorList="report.objects[selectedResourceIndex].minimalErrors" :attList="report.objects[selectedResourceIndex].minimalChecked" :att-list-name="'Required Attributes'"> </log-error-list>
                </div>
                <div>
                    <span class="caption">Coding</span>
                    <span v-if="report.objects[selectedResourceIndex].codingErrors.length === 0"><img src="../../assets/check.png"></span>
                    <span v-else><img src="../../assets/cross.png"></span>
                    <log-error-list :errorList="report.objects[selectedResourceIndex].codingErrors"> </log-error-list>
                </div>
            </div>


        </div>
    </div>
</template>

<script>
    import LogErrorList from "./LogErrorList"

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
        components: { LogErrorList },
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
    .boxed {
        border: 1px solid rgba(0, 0, 0, 0.8);
        position: relative;
        left: -60px;
    }
</style>
