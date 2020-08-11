<template>
    <div>
        <div v-if="report && !noInspectLabel" class="has-cursor">
            <span v-if="closed"><img src="../../assets/arrow-right.png" @click.stop="closed = !closed"/></span>
            <span v-else><img src="../../assets/arrow-down.png" @click.stop="closed = !closed"/></span>
            <span @click.stop="closed = !closed">Inspect</span>
        </div>
        <div v-if="!closed">
            <!--  report is the Inspection report and not TestReport    -->
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
                <div v-if="history.length > 0" class="solid-boxed">
                    <div class="nav-buttons">
                        <div v-if="moreToTheLeft()" class="tooltip left-arrow-position">
                            <img id="left-button" class="selectable" src="../../assets/left-arrow.png" @click="left()"/>
                            <span class="tooltiptext">Previous</span>
                        </div>
                    </div>
                    <div class="details">History</div>
                    <div class="vdivider"></div>

                </div>

                <!--  BASE OBJECT     -->
                <div>
                    <span class="caption">Focus Object:</span>
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
                <span class="caption">Related: </span>
                <span>(referenced by Focus Object)</span>
                <div class="vdivider"></div>
                <div class="grid-container">
                    <span v-for="(resource, resourcei) in report.objects"
                        :key="resource + resourcei">
                        <div class="grid-item">
                            <span v-bind:class="objectDisplayClass(resource)"
                                @click="selectedResourceIndex = resourcei">
                                {{ resource.name }} ({{ resource.relation }})
                                <span class="tooltip">
                                    <!--   loadAnalysisForObjectAndAddHistory(report.objects[resourcei].url, resourcei)    -->
                                    <img id="focus" class="selectable" src="../../assets/focus.png" @click.stop="loadAnalysisFromEventContext(report.objects[resourcei].url, report.objects[resourcei].eventContext, true)">
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
        </div>
      <router-view></router-view>
    </div>
</template>

<script>
    import LogObjectDisplay from "./LogObjectDisplay"

    export default {
        data() {
            return {
                selectedResourceIndex: null,  // -1 is focus object.  0 or greater is a related object.
                history: [],   // {url: report.base.url, eventId: eventId } - history[0] is never removed - it is the base object
                closed: false,
            }
        },
        methods: {
            moreToTheLeft() {
                return this.history.length > 1
            },
            historyClear() {
                this.history.length = 0
            },
            historyPush(url, eventId) {
                this.history.push({ url: url, eventId: eventId})
            },
            empty() {
                return this.history.length === 0
            },
            left() {  // make previous object the focus
                if (this.moreToTheLeft()) {
                    this.pop()
                    const history = this.peek()
                    if (!history)
                        this.loadAnalysis()
                    else if (history.eventId)
                        this.loadAnalyisFromEventId(history.url, history.eventId, false)
                    else
                        this.loadAnalysis()
                }
            },
            peek() {
                if (this.history.length === 0)
                    return null
                return this.history[this.history.length - 1]
            },
            pop() {
                if (this.history.length === 0)
                    return null
                return this.history.pop()
            },
            async  loadAnalysis() {
                await this.loadAnalysis2()
                this.selectedResourceIndex = -1
                this.historyClear()
                //console.log(`using analysis`)
                this.historyPush(this.report.base.url, null)
            },
            async loadAnalysis2() {
                if (this.eventId) {
                    //console.log(`loadAnalysis2 for ${this.eventId}`)
                    //console.log(`theUrl=${this.theUrl}`)
                    await this.$store.dispatch('getLogEventAnalysis', {
                        channel: this.channelId,
                        session: this.sessionId,
                        eventId: this.eventId,
                        requestOrResponse: this.requestOrResponse
                    })
                }
            },
            async loadAnalysisFromEventContext(url, eventContext, addToHistory) {
                //console.log(`loadAnalysisFromEventContext url=${url} eventContext.eventId=${eventContext.eventId}`)
                const eventId = eventContext ? eventContext.eventId : null
                await this.loadAnalyisFromEventId(url, eventId, addToHistory)
            },
            async loadAnalyisFromEventId(url, eventId, addToHistory) {
                //console.log(`loadAnalyisFromEventContext for ${eventId}`)
                //console.log(`url=${url}`)
                // if (url.startsWith('http'))
                //     await this.loadAnalysisForObject(url)
                // else
                await this.$store.dispatch('getLogEventAnalysis', {channel: this.channelId, session: this.sessionId, eventId: eventId, requestOrResponse: this.requestOrResponse, url: url})
                if (addToHistory)
                    this.historyPush(url, eventId)
                this.index = this.history.length - 1
            },
            loadAnalysisForObject(resourceUrl) {
                console.log(`loadAnalysisForObject ${resourceUrl}`)
                this.$store.dispatch('getLogEventAnalysisForObject', {
                    resourceUrl: resourceUrl,
                    gzip: this.gzip,
                    ignoreBadRefs: this.ignoreBadRefs,
                    eventId: this.eventId
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
                this.historyPush(resourceUrl, null)
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
            urlAnalysis(url) {
                console.log(`LogAnalysisReport url is ${url}`)
                this.loadAnalysisForObject(url)
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
        },
        created() {
          console.log(`created`)
            if (this.theUrl) {
                console.log(`created with url`)
                this.urlAnalysis(this.theUrl);
            }
            else if (this.$route.query.url) {
              console.log(`created with log from param`)
              this.urlAnalysis(this.$route.query.url)
            }
            else
                this.loadAnalysis()
            this.closed = this.initiallyClosed
        },
        watch: {
            'eventId': 'loadAnalysis',
            'requestOrResponse': 'loadAnalysis',
            'theUrl': 'urlAnalysis'
        },
        props: [  // pass eventId OR theUrl
            'sessionId', 'channelId', 'eventId',
          'theUrl', // this is the URL of a Resource inside a Bundle
          'gzip', 'useProxy', "requestOrResponse", "ignoreBadRefs",
            'initiallyClosed', 'noInspectLabel',
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
