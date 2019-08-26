<template>
    <div>
        <div class="window-title">Asbestos</div>
        <div class="grid-container">
            <router-view name="session" class="main"></router-view>
            <div class="control-panel">
                <session-control-panel class="solid-boxed"></session-control-panel>
                <div class="vdivider"></div>
                <environment-control-panel class="solid-boxed"></environment-control-panel>
                <div class="vdivider"></div>
                <channel-control-panel class="solid-boxed"></channel-control-panel>
                <div class="vdivider"></div>
                <log-control-panel class="solid-boxed"></log-control-panel>
                <div class="vdivider"></div>
                <test-session-control-panel class="solid-boxed"></test-session-control-panel>
                <div class="vdivider"></div>
                <test-control-panel class="solid-boxed"></test-control-panel>
            </div>
        </div>
    </div>
</template>

<script>
    import EnvironmentControlPanel from "./EnvironmentControlPanel"
    import LogControlPanel from "./LogControlPanel"
    import TestSessionControlPanel from "./TestSessionControlPanel"
    import SessionControlPanel from "./SessionControlPanel"
    import ChannelControlPanel from "./ChannelControlPanel"
    import TestControlPanel from "./TestControlPanel"

    export default {
        data() {
            return {
                testSession: 'default',
                testSessions: null
            }
        },
        created() {

        },
        methods: {
        },
        computed: {
        },
        watch: {
        },
        name: 'TopLayout',
        components: {
            EnvironmentControlPanel,
            LogControlPanel,
            TestSessionControlPanel,
            SessionControlPanel,
            ChannelControlPanel,
            TestControlPanel
        }
    }
</script>
<style scoped>
    .window-title {
        font-weight: bold;
        font-size: larger;
    }
    .grid-container {
        display: grid;
        grid-template-areas:
                'header header header header'
                'body body body controls';
        /*grid-template-columns: max-content fit-content(10%);*/
    }
    .title {
        grid-area: header;
    }
    .main {
        grid-area: body;
    }
    .control-panel {
        grid-area: controls;
        /*grid-column: 2;*/
        /*grid-row: 2;*/
        position: absolute;
        right: 4px;
    }
</style>
// these are shared across the tool
<style>
    .control-panel-item-title {
        font-weight: bold;
        cursor: pointer;
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
        /*border-bottom: 1px dotted black;*/
    }
    .tooltip .tooltiptext {
        visibility: hidden;
        width: 120px;
        background-color: blue;
        color: #fff;

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
</style>
