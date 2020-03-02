<template>
    <div v-if="report" class="boxed">
        <div class="vdivider"></div>
        <div class="main-caption">Inspector</div>

        <!--    ERRORS    -->
        <div v-if="report.errors && report.errors.length > 0 && report.errors[0] !== null">  <!--  don't know why the null check is needed but it is  -->
            <span class="caption inspector-error">Errors:</span>
            <div class="vdivider"></div>
            <div v-for="(err, erri) in report.errors"
                :key="err + erri">
                <div class="inspector-error">
                    {{ report.errors[erri]}}
                </div>
            </div>
            <hr />
        </div>
        <div class="vdivider"></div>

        <!--    WARNINGS    -->
        <div v-if="report.warnings && report.warnings.length > 0 && report.warnings[0] !== null">  <!--  don't know why the null check is needed but it is  -->
            <span class="caption">Warnings:</span>
            <div class="vdivider"></div>
            <div v-for="(err, erri) in report.warnings"
                 :key="err + erri">
                <div>
                    {{ report.warnings[erri]}}
                </div>
            </div>
            <hr />
        </div>
        <div class="vdivider"></div>

        <div v-if="isRequest">
            Content displayed is from Request message. Resources referenced by full URLs are pulled from their servers.
        </div>
        <div v-else>
            Resource IDs are extracted from the Response message. Content shown comes from the server
            via separate GETs.
        </div>
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
                            <img id="focus" class="selectable" src="../../assets/focus.png" @click.stop="loadAnalysisForObjectAndAddHistory(report.objects[resourcei].url, resourcei)">
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
        <div v-else-if="selectedResourceIndex === -1 && report.base">
            <log-object-display :report="report.base"> </log-object-display>
        </div>

        <!--  RELATED OBJECT DETAILS -->
        <div v-else-if="selectedResourceIndex > -1">
            <log-object-display :report="report.objects[selectedResourceIndex]"> </log-object-display>
        </div>
    </div>
</template>

<script>
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
            historyClean() {  // remove everything past index
                this.history.length = this.index + 1
            },
            historyPush(url) {
                this.historyClean()
                this.history.push(url)
            },
            async  loadAnalysis() {
                await this.loadAnalysis2()
                this.selectedResourceIndex = -1
                this.history.length = 0
                //console.log(`using analysis`)
                this.history.push(this.report.base.url)
            },
            async loadAnalysis2() {
                if (this.eventId)
                    await this.$store.dispatch('getLogEventAnalysis', {channel: this.channelId, session: this.sessionId, eventId: this.eventId, requestOrResponse: this.requestOrResponse})
            },
            loadAnalysisForObject(resourceUrl) {
                this.$store.dispatch('getLogEventAnalysisForObject', {
                    channel: this.channelId,
                    session: this.sessionId,
                    eventId: this.eventId,
                    requestOrResponse: this.requestOrResponse,
                    resourceUrl: resourceUrl,
                    gzip: this.gzip,
                    useProxy: this.useProxy,
                    ignoreBadRefs: this.ignoreBadRefs
                })
                this.selectedResourceIndex = -1
            },
            loadAnalysisForObjectAndAddHistory(resourceUrl, index) {
                if (this.report.objects[index].url === 'Contained') {
                    let subReport = {}
                    subReport.base = this.report.objects[index]
                        //this.report.objects[index]
                    this.$store.commit('setAnalysis', subReport)
                } else {
                    this.loadAnalysisForObject(resourceUrl)
                }
                this.historyPush(resourceUrl)
                this.index = this.history.length - 1
            },
            objectDisplayClass: function (resource) {
                const defined = ['DocumentManifest', 'DocumentReference', 'Patient', 'Binary']
                return {
                    manifest: resource.name === 'DocumentManifest',
                    ref: resource.name === 'DocumentReference',
                    patient: resource.name === 'Patient',
                    binary: resource.name === 'Binary',
                    other: defined.indexOf(resource.name) < 0
                }
            },
            urlAnalysis() {
                // console.log(`LogAnalysisReport url is ${this.theUrl}`)
                this.loadAnalysisForObject(this.theUrl)
            }
        },
        computed: {
            isRequest() {
                return this.requestOrResponse === 'request'
            },
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
            if (this.theUrl)
                this.urlAnalysis()
            else
                this.loadAnalysis()
        },
        watch: {
            'eventId': 'loadAnalysis',
            'requestOrResponse': 'loadAnalysis',
            'theUrl': 'urlAnalysis'
        },
        props: [  // pass eventId OR theUrl
            'sessionId', 'channelId', 'eventId', 'theUrl', 'gzip', 'useProxy', "requestOrResponse", "ignoreBadRefs",
        ],
        components: { LogObjectDisplay },
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
    .other {
        background-color: bisque;
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
    .inspector-error {
        color: indianred;
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
