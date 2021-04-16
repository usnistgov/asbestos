<template>
    <div>
        <div>
        <div class="left window-title-bar">
            <span class="window-title">NIST FHIR<img src="../../assets/copyright.png"> Toolkit</span>
            <span class="title-divider"> </span>

            <span>{{ projectVersion }}</span>

            <span class="title-divider"> </span>
            <span class="selectable" @click="go('/home')">Home</span>

            <div class="divider"></div>
            <span class="selectable" @click="go('/mhdtesting')">MHD Testing</span>

            <div class="divider"></div>
            <span class="selectable" @click="go('/configurations')">Configurations</span>

            <div class="divider"></div>
            <span class="selectable" @click="go('/about')">About</span>

            <div class="divider"></div>
            <span class="selectable" @click="go('/setup')">Setup</span>

            <div class="divider"></div>
            <span v-if="this.asbts_UserProps.signedIn === false" class="selectable" @click="go('/admin')">Admin</span>

            <div class="divider"></div>
            <span v-if="this.asbts_UserProps.signedIn === true" class="selectable" @click="signOut">Sign Out</span>

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
                <general-control-panel class="solid-boxed"> </general-control-panel>
<!--                <getter-control-panel class="solid-boxed"> </getter-control-panel>-->
                <div class="vdivider"></div>
<!--                <channel-log-control-panel class="solid-boxed"> </channel-log-control-panel>-->
<!--                <div class="vdivider"></div>-->
                <test-control-panel2 class="solid-boxed"> </test-control-panel2>
                <div class="vdivider"></div>
            </div>
        </div>
    </div>
</template>

<script>
    import SessionControlPanel from "./SessionControlPanel"
    import ChannelControlPanel from "./ChannelControlPanel"
    import GeneralControlPanel from "./GeneralControlPanel";
    import TestControlPanel2 from "./TestControlPanel2"
    // import ChannelLogControlPanel from "./ChannelLogControlPanel"
    // import GetterControlPanel from "./GetterControlPanel";
    // import DebugControlPanel from "./DebugControlPanel"
    import {PROJECTVERSION, ASBTS_USERPROPS} from "../../common/http-common";

    export default {
        data() {
            return {
                testSession: 'default',
                testSessions: null,
                error: null,
                asbts_UserProps: ASBTS_USERPROPS
            }
        },
        beforeRouteEnter(to, from, next) {
            if (to.path === '/')
                next('/home')
            else
                next()
        },
        created() {
            this.$store.dispatch('loadProxyBase')
        },
        methods: {
            go(there) {
                this.$router.push(there)
            },
            clearErrors() {
                this.$store.commit('clearError')
            },
            signOut() {
                this.asbts_UserProps.signedIn = false
                this.asbts_UserProps.bauser = ""
                this.asbts_UserProps.bapw = ""
            }
        },
        computed: {
            projectVersion() {
                return PROJECTVERSION
            }
        },
        watch: {
        },
        name: 'TopLayout',
        components: {
            SessionControlPanel,
            ChannelControlPanel,
            GeneralControlPanel,
            TestControlPanel2,
            // ChannelLogControlPanel,
            // GetterControlPanel
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
        grid-template-columns: minmax(0, 1fr) 250px;
    }
    .error-grid-container {
        display: grid;
        grid-template-columns: 20px .9fr;
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
        right: 9px;
        text-align: left;
    }
</style>
// these are shared across the tool
<style>
.indent {
  text-indent: 50px;
}
.indent2 {
  text-indent: 100px;
}
    .system-error {
        font-weight: bold;
        font-size: larger;
        background-color: red;
    }
    .control-panel-font {
        font-size: small;
    }
    .bold {
        font-weight: bold;
    }
    .big-bold {
        font-weight: bold;
        font-size: large;
    }
    .control-panel-item-title {
        margin-top: 4px;
        margin-bottom: 4px;
        font-weight: bold;
        /*cursor: pointer;*/
        /*text-decoration: underline;*/
    }
    .divider{
        width:5px;
        height:auto;
        display:inline-block;
    }
    .divider80{
        width:80px;
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
        padding: 27px;
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
    .underline {
      text-decoration: underline;
    }
    .selectable {
        cursor: pointer;
    }
    .pointer-cursor {
        cursor: pointer;
    }
    .has-cursor {
        cursor: pointer;
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
