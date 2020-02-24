<template>
    <div class="left">
        <div class="tool-title">Getter</div>
        <div class="vdivider"></div>
        <div class="vdivider"></div>

        <input type="radio" id="plain" value="plain" v-model="selection">
        <label for="plain">Execute arbitrary GET against a FHIR server and display results in the Inspector.</label>
        <br />
        <input type="radio" id="capstmt" value="capstmt" v-model="selection">
        <label for="capstmt">GET CapabilityStatement from selected Channel ({{channelId}})</label>

        <div class="work-area">
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
                    <input type="checkbox" id="doGzip" v-model="gzip">
                    <label for="doGzip">GZip?</label>
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
            <div v-else-if="selection === 'capstmt'">
<!--                <input type="checkbox" id="doValidate2" v-model="validate">-->
<!--                <label for="doValidate2">Validate against configured FHIR Validation Server? ({{this.$store.state.log.validationServer}})</label>-->
<!--                <br />-->
                <input type="checkbox" id="doGzip1" v-model="gzip">
                <label for="doGzip1">GZip?</label>
                <input type="checkbox" id="useProxy1" v-model="useProxy">
                <label for="useProxy1">Use Proxy? (must be proxy URL for this to have any effect)</label>
                <div class="left">
                    <button class="left" @click="runCapStmt()">Run</button>
                    <div>{{validation}}</div>
                    <div v-if="capStmt" class="request-response" :key="reCapStmt">
                        <log-analysis-report
                                :session-id="sessionId"
                                :channel-id="channelId"
                                :the-url="`${$store.getters.getProxyBase({channelId: channelId, sessionId: sessionId})}/metadata`"
                                :gzip="gzip"
                                :use-proxy="useProxy"> </log-analysis-report>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import LogAnalysisReport from "../logViewer/LogAnalysisReport";
    //import Vue from 'vue';
    //Vue.forceUpdate();

    export default {
        data() {
            return {
                url: null,
                theUrl: null,
                gzip: false,
                useProxy: false,
                rerenderkey: 0,
                selection: null,
                validate: false,
                reCapStmt: 0,
                capStmt: null,
                validation: null,
            }
        },
        methods : {
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
            runValidation() {
                this.$store.dispatch('getValidation')
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
