<template>
    <div class="left">
        <div class="tool-title">Get/Inspect/Validate</div>
        <div class="vdivider"></div>
        <div class="vdivider"></div>

        <input type="checkbox" id="doGzip" v-model="gzip">
        <label for="doGzip">Use GZip?</label>
        <br />
        <br />


<!--        <input type="radio" id="plain" value="plain" v-model="selection">-->
<!--        <label for="plain">Execute arbitrary GET against a FHIR server and display results in the Inspector.</label>-->
<!--        <br />-->
        <input type="radio" id="capstmt" value="capstmt" v-model="selection">
        <label for="capstmt">GET CapabilityStatement from selected Channel ({{channelId}})</label>
<!--        <br />-->
<!--        <input type="radio" id="valcapstmt" value="valcapstmt" v-model="selection">-->
<!--        <label for="valcapstmt">Validate CapabilityStatement from selected Channel ({{channelId}})</label>-->


            <div v-if="selection === 'plain'">
                <div class="vdivider"></div>
                <div class="vdivider"></div>
                <div class="vdivider"></div>
                <div class="vdivider"></div>

                <div class="left">
                    <span>URL</span>
                    <div class="divider"> </div>
                    <input v-model="url" size="80" v-on:keyup="maybeRun">
                    <div class="vdivider"> </div>
                    <input type="checkbox" id="useProxy" v-model="useProxy">
                    <label for="useProxy">Use Proxy? (must be proxy URL for this to have any effect)</label>
<!--                    <br />-->
<!--                    <input type="checkbox" id="doValidate1" v-model="validate">-->
<!--                    <label for="doValidate1">Validate against configured FHIR Validation Server?</label>-->

                    <div class="vdivider"> </div>
                </div>
                <div class="left">
                    <button class="left" @click="run()">Run</button>
                </div>
                <div v-if="theUrl" class="request-response" :key="rerenderkey">
                    <log-analysis-report
                            :session-id="sessionId"
                            :channel-id="channelId"
                            :the-url="theUrl"
                            :gzip="gzip"
                            :use-proxy="useProxy"> </log-analysis-report>
                </div>
            </div>
        <br />
        <button class="left" @click="runCapStmt()">GET</button> CapabilityStatement from selected Channel ({{channelId}})

<!--                <input type="checkbox" id="doValidate2" v-model="validate">-->
<!--                <label for="doValidate2">Validate against configured FHIR Validation Server? ({{$store.state.log.validationServer}})</label>-->
<!--                <br />-->
                <div class="left">
<!--                    <button class="left" @click="runCapStmt()">Run</button>-->
<!--                    <div>{{validation}}</div>-->
                    <div v-if="capStmt" class="request-response" :key="reCapStmt">
                        <div class="left">
                            <span class="selectable left" @click="toggleInspectorOpen()">Inspector</span>
                            <span v-if="inspectorOpen">
                                <img src="../../assets/arrow-down.png" @click="toggleInspectorOpen()">
                                <log-analysis-report
                                    :session-id="sessionId"
                                    :channel-id="channelId"
                                    :the-url="`${$store.getters.getProxyBase({channelId: channelId, sessionId: sessionId})}/metadata`"
                                    :gzip="gzip"
                                    :use-proxy="useProxy"
                                    :ignore-bad-refs="true"> </log-analysis-report>
                            </span>
                            <span v-else>
                                <img src="../../assets/arrow-right.png" @click="toggleInspectorOpen()">
                            </span>
                        </div>
                    </div>
                </div>

            <div v-if="selection === 'valcapstmt'">
<!--                <label for="doValidate2">Using configured FHIR Validation Server ({{$store.state.log.validationServer}})</label>-->
                <br />
                <input type="checkbox" id="doGzip2" v-model="gzip">
                <label for="doGzip2">GZip?</label>
                <div class="left">
                    <button class="left" @click="runCapStmtValidation()">Run</button>
                    <div v-if="validate" class="request-response" :key="reValidation">
                        <log-analysis-report
                                :session-id="sessionId"
                                :channel-id="channelId"
                                :the-url="validateCallUrl"
                                :gzip="gzip"
                                :use-proxy="useProxy"
                                :ignore-bad-refs="true"> </log-analysis-report>
                    </div>
                </div>
            </div>

    </div>
</template>

<script>
    import LogAnalysisReport from "../logViewer/LogAnalysisReport";
    import {FHIRTOOLKITBASEURL} from "../../common/http-common";

    export default {
        data() {
            return {
                url: null,
                theUrl: null,
                gzip: true,
                useProxy: false,
                rerenderkey: 0,
                selection: null,
                validate: false,
                reCapStmt: 0,
                capStmt: null,
                reValidation: 0,
                validation: null,
                inspectorOpen: true,
            }
        },
        methods : {
            toggleInspectorOpen() {
                this.inspectorOpen = !this.inspectorOpen
            },
            maybeRun(key) {
               if (key.keyCode === 13) // enter
                   this.run()
            },
            run() {
                this.theUrl = this.url
                this.rerenderkey += 1
            },
            runCapStmt() {
                this.capStmt = true
                this.reCapStmt += 1
            },
            runCapStmtValidation() {
                this.validate = true
                this.reValidation += 1
            },
            runValidation() {
                this.$store.dispatch('getValidation')
            },
            async validateCall(resourceType, url) {
                console.log('url is ' + url)
                const valUrl =`${FHIRTOOLKITBASEURL}/validate/${resourceType}?${url}`
                console.log(`valUrl is ${valUrl}`)
            }
        },
        computed: {
             validateCallUrl() {
                 const url = this.$store.getters.getProxyBase({channelId: this.channelId, sessionId: this.sessionId}) + '/metadata'
                 return this.validateCall('CapabilityStatement', url)
             },
        },
        created() {
            if (this.$store.state.log.validationServer === null)
                this.$store.dispatch('getValidationServer')
        },
        props: [
            'sessionId', 'channelId'
        ],
        components: { LogAnalysisReport },
        name: "Getter"
    }
</script>

<style scoped>
    .request-response {
        position: relative;
        left: 60px;
        text-align: left;
    }
    .work-area {
        border: 2px black solid;
    }
</style>
