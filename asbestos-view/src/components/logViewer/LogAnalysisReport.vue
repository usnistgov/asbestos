<template>
    <div v-if="report" class="boxed">
        <div class="vdivider"></div>
        <div class="main-caption">Inspector</div>
        <!--    ERRORS    -->
        <div v-if="report.errors.length > 0 && report.errors[0] !== null">  <!--  don't know why the null check is needed but it is  -->
            <span class="caption">Errors:</span>
            <div class="vdivider"></div>
            <div v-for="(err, erri) in report.errors"
                :key="err + erri">
                <div>
                    {{ report.errors[erri]}}
                </div>
            </div>
            <hr />
        </div>
        <div class="vdivider"></div>

        <div>Resource IDs are extracted from the Response message. Content shown comes directly from the server
            via separate GETs.</div>
        <div class="vdivider"></div>

        <!--   history navigation   -->
        <div v-if="history.length > 1" class="solid-boxed">
            <div class="nav-buttons">
                <div v-if="moreToTheLeft" class="tooltip left-arrow-position">
                    <img id="left-button" class="selectable" src="../../assets/left-arrow.png" @click="left()"/>
                    <span class="tooltiptext">Previous</span>
                </div>
                <div v-if="moreToTheRight" class="tooltip right-arrow-position">
                    <img id="right-button" class="selectable" src="../../assets/right-arrow.png" @click="right()"/>
                    <span class="tooltiptext">Next</span>
                </div>
            </div>
            <div class="details">Navigate History</div>
            <div class="vdivider"></div>

        </div>

        <!--  BASE OBJECT     -->
        <div>
            <span class="caption">Focus Object:</span>
<!--            <span v-if="report.source">{{ report.source }}</span>-->
            <div class="vdivider"></div>
            <div class="grid-container">
                <span v-if="report.base">
                    <div class="grid-item">
                        <span v-bind:class="objectDisplayClass(report.base)"
                              @click="selectedResourceIndex = -1">
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
                    <span v-bind:class="objectDisplayClass(resource)"
                          @click="selectedResourceIndex = resourcei">
                        {{ resource.name }} ({{ resource.relation }})
                        <span class="tooltip">
                            <img id="focus" class="selectable" src="../../assets/focus.png" @click.stop="loadAnalysisForObjectAndAddHistory(report.objects[resourcei].url)">
                            <span class="tooltiptext">Focus</span>
                        </span>
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
            <div v-if="report.base.name === 'Binary'">
                <div>Contents: <a v-bind:href="report.base.binaryUrl" target="_blank">open</a> (in new browser tab) </div>
                <div>Contents direct from server: <a v-bind:href="report.base.url" target="_blank">open</a> (in new browser tab) </div>
            </div>
            <log-atts :attMap="report.base.atts"> </log-atts>
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
            <div v-if="report.objects[selectedResourceIndex].name === 'Binary'">
                <span>Contents: <a v-bind:href="report.objects[selectedResourceIndex].binaryUrl" target="_blank">open</a> (in new browser tab) </span>
            </div>
            <log-atts :attMap="report.objects[selectedResourceIndex].atts"> </log-atts>
        </div>
    </div>
</template>

<script>
    import LogErrorList from "./LogErrorList"
    import LogAtts from "./LogAtts"
    import LogObjectDisplay from "./LogObjectDisplay"

    export default {
        data() {
            return {
                selectedResourceIndex: null,  // -1 is focus object.  0 or greater is a related object.
                history: [],   // report.base.url
                index: 0,      // in history
            }
        },
        methods: {
            left() {
                if (this.index > 0) {
                    this.index = this.index - 1
                    this.loadAnalysisForObject(this.history[this.index])
                }
            },
            right() {
                if (this.index + 1 < this.history.length) {
                    this.index = this.index + 1
                    this.loadAnalysisForObject(this.history[this.index])
                }
            },
            historyPush(url) {
                this.history.push(url)
            },
            historyPeek() {
                if (this.history.length === 0)
                    return null
                return this.history[this.history.length - 1]
            },
            async  loadAnalysis() {
                await this.loadAnalysis2()
                this.selectedResourceIndex = -1
                this.history.length = 0
                //console.log(`using analysis`)
                this.history.push(this.report.base.url)
            },
            async loadAnalysis2() {
                await this.$store.dispatch('getLogEventAnalysis', {channel: this.channelId, session: this.sessionId, eventId: this.eventId})
            },
            loadAnalysisForObject(resourceUrl) {
                //console.log(`loadForObject ${resourceUrl}`)
                this.$store.dispatch('getLogEventAnalysisForObject', resourceUrl)
                this.selectedResourceIndex = -1
            },
            loadAnalysisForObjectAndAddHistory(resourceUrl) {
                this.loadAnalysisForObject(resourceUrl)
                this.historyPush(resourceUrl)
                this.index = this.history.length - 1
            },
            objectDisplayClass: function (resource) {
                return {
                    manifest: resource.name === 'DocumentManifest',
                    ref: resource.name === 'DocumentReference',
                    patient: resource.name === 'Patient',
                    binary: resource.name === 'Binary'
                }
            },
        },
        computed: {
            report() {
                // content from gov.nist.asbestos.analysis.AnalysisReport.Report
                return this.$store.state.log.analysis
            },
            moreToTheLeft() {
                return this.index > 0
            },
            moreToTheRight() {
                return this.index + 1 < this.history.length
            }
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
        components: { LogErrorList, LogAtts, LogObjectDisplay },
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
.binary {
    background-color: greenyellow;
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
.nav-buttons {
    text-align: left;
}
.left-arrow-position {
    position: absolute;
    left: 160px;
}
.right-arrow-position {
    position: absolute;
    left: 200px;
}
.details {
    font-size: smaller;
}
</style>
