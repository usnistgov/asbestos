<template>
    <div>
        <div>
        <div class="left window-title-bar">
            <span class="window-title">NIST FHIR<img src="../../assets/copyright.png"> Toolkit</span>
            <span class="title-divider"> </span>
            <span class="selectable" @click="go('/home')">Home</span>
            <div class="divider"></div>
            <span class="selectable" @click="go('/about')">About</span>
        </div>
        </div>
        <div class="grid-container">
            <div>
                <div v-if="$store.state.base.errors.length > 0" class="error-grid-container">
                    <img class="error-grid-close"
                         src="../../assets/close-button.png"
                         @click="clearErrors()"
                    >
                    <div  class="error-grid-contents left soft-boxed">
                        <div v-for="(err, erri) in $store.state.base.errors"
                            :key="err+erri">
                            <img src="../../assets/error.png"> {{ err }}
                        </div>
                    </div>
                </div>
                <router-view> </router-view>  <!--  for menu content   -->
                <router-view name="session" class="main"> </router-view>
            </div>
            <div class="control-panel control-panel-font">
                <session-control-panel class="solid-boxed"> </session-control-panel>
                <div class="vdivider"></div>
                <channel-control-panel class="solid-boxed"> </channel-control-panel>
                <div class="vdivider"></div>
<!--                <log-control-panel class="solid-boxed"></log-control-panel>-->
<!--                <div class="vdivider"></div>-->
                <channel-log-control-panel class="solid-boxed"> </channel-log-control-panel>
                <div class="vdivider"></div>
                <test-control-panel class="solid-boxed"> </test-control-panel>
                <div class="vdivider"></div>
<!--                <debug-control-panel class="solid-boxed"></debug-control-panel>-->
            </div>
        </div>
    </div>
</template>

<script>
    import SessionControlPanel from "./SessionControlPanel"
    import ChannelControlPanel from "./ChannelControlPanel"
    import TestControlPanel from "./TestControlPanel"
    import ChannelLogControlPanel from "./ChannelLogControlPanel"
    // import DebugControlPanel from "./DebugControlPanel"

    export default {
        data() {
            return {
                testSession: 'default',
                testSessions: null,
                error: null,
            }
        },
        beforeRouteEnter(to, from, next) {
            if (to.path === '/')
                next('/home')
            else
                next()
        },
        created() {
            // this.$store.commit('setError', 'Oops')
            // this.$store.commit('setError', 'Oopsie')
        },
        methods: {
            go(there) {
                this.$router.push(there)
            },
            clearErrors() {
                this.$store.commit('clearError')
            },
        },
        computed: {
        },
        watch: {
        },
        name: 'TopLayout',
        components: {
            SessionControlPanel,
            ChannelControlPanel,
            TestControlPanel,
            ChannelLogControlPanel,
            // DebugControlPanel,
        }
    }




</script>
<style scoped>
    .window-title-bar {
        background-color: cornflowerblue;
        padding-top: 10px;
        padding-bottom: 10px;
    }
    .window-title {
        font-weight: bold;
        font-size: larger;
        text-align: left;
    }
    .title-divider{
        width:20px;
        height:auto;
        display:inline-block;
    }
    .larger {
        font-size: larger;
    }
    .grid-container {
        display: grid;
        grid-template-columns: minmax(0, 1fr) 200px;
    }
    .error-grid-container {
        display: grid;
        grid-template-columns: 20px 1fr;
    }
    .error-grid-close {
        grid-column: 1;
    }
    .error-grid-contents {
        grid-column: 2;
    }
    .title {
        grid-area: header;
    }
    .main {
        /*grid-area: body;*/
    }
    .control-panel {
        /*grid-area: controls;*/
        /*grid-column: 2;*/
        /*grid-row: 2;*/
        position: absolute;
        right: 4px;
        text-align: left;
    }

</style>
// these are shared across the tool
<style>
    .control-panel-font {
        font-size: small;
    }
    .bold {
        font-weight: bold;
    }
    .control-panel-item-title {
        font-weight: bold;
        /*cursor: pointer;*/
        /*text-decoration: underline;*/
    }
    .divider{
        width:5px;
        height:auto;
        display:inline-block;
    }
    .vdivider{
        height:3px;
        width:auto;
    }
    .right {
        display:inline-block;
        float: right;
    }
    .left {
        text-align: left;
    }
    .soft-boxed {
        border: thin solid lightgray;
    }
    .boxed {
        border: 1px dotted black;
    }
    .solid-boxed {
        border: 1px solid black;
    }
    .panel {
        padding: 20px;
    }
    .tooltip {
        position: relative;
        display: inline-block;
    }
    .tooltip .tooltiptext {
        visibility: hidden;
        width: 120px;
        background-color: lightgray;
        color: black;

        bottom: 100%;
        left: 50%;
        margin-left: -60px;

        /* Position the tooltip */
        position: absolute;
        z-index: 1;
    }
    .tooltip:hover .tooltiptext {
        visibility: visible;
    }
    .tool-title {
        font-weight: bold;
        font-size: larger;
        text-align: left;
    }
    .selectable {
        cursor: pointer;
        text-decoration: underline;
    }
    .has-cursor {
        cursor: pointer;
    }
    .pass {
        background-color: lightgreen;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .fail {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .error {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .warning {
        background-color: #F6C6CE;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .not-run {
        background-color: lightgray;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        border-radius: 25px;
    }
    .instruction {
        text-align: left;
        padding-top: 5px;
        padding-bottom: 5px;
    }
    .second-instruction {
        text-align: left;
        /*padding-top: 5px;*/
        padding-bottom: 5px;
    }
</style>
