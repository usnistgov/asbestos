<template>
    <div>
        <div class="vdivider"></div>
        <div class="main-caption">
            {{ report.name }}
            <span v-if="report.relation">
                ({{ report.relation }})
            </span>
        </div>
        <div v-if="report.url">
            <span class="caption"> Ref:</span>
            {{ report.url }}
        </div>

        <div v-if="report.name === 'DocumentManifest' || report.name === 'DocumentReference'">
            <div>
                <span>Comprehensive Metadata </span>
                <span v-if="report.isComprehensive"><img src="../../assets/check.png"></span>
                <span v-else><img src="../../assets/cross.png"></span>
                <div class="divider"></div>
                <log-error-list :errorList="report.comprehensiveErrors" :attList="report.comprehensiveChecked" :att-list-name="'Required Attributes'"> </log-error-list>
            </div>
            <div>
                <span>Minimal Metadata </span>
                <span v-if="report.isMinimal"><img src="../../assets/check.png"></span>
                <span v-else><img src="../../assets/cross.png"></span>
                <div class="divider"></div>
                <log-error-list :errorList="report.minimalErrors" :attList="report.minimalChecked" :att-list-name="'Required Attributes'"> </log-error-list>
            </div>
            <div>
                <span class="caption">Coding</span>
                <span v-if="report.codingErrors.length === 0"><img src="../../assets/check.png"></span>
                <span v-else><img src="../../assets/cross.png"></span>
                <log-error-list :errorList="report.codingErrors"> </log-error-list>
            </div>
        </div>
        <div v-if="report.name === 'Binary'">
            <div>Contents: <a v-bind:href="report.binaryUrl" target="_blank">open</a> (in new browser tab) </div>
            <div>Contents direct from server: <a v-bind:href="report.url" target="_blank">open</a> (in new browser tab) </div>
        </div>
        <log-atts :attMap="report.atts"> </log-atts>

    </div>
</template>

<script>
    import LogErrorList from "./LogErrorList"
    import LogAtts from "./LogAtts"

    export default {
        props: [
            'report'
        ],
        components: { LogErrorList, LogAtts },
        name: "LogObjectDisplay"
    }
</script>

<style scoped>
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
