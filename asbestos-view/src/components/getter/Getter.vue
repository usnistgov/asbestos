<template>
    <div>
        <div class="tool-title">Getter</div>
        <div class="vdivider"></div>
        <div class="vdivider"></div>
        <div class="left">
        Execute arbitrary GET against a FHIR server and display results in the Inspector.
        </div>

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
                :gzip="gzip"> </log-analysis-report>
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
                rerenderkey: 0,
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
            }
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
</style>
