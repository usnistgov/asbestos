<template>
    <div>
        <h3>Asbestos</h3>
        <router-view name="session"></router-view>
    </div>
</template>

<script>
   // import SessionView from "./SessionView";

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
          //  SessionView
        }
    }
</script>
// these are shared across the tool
<style>
    .divider{
        width:5px;
        height:auto;
        display:inline-block;
    }
    .right {
        display:inline-block;
        float: right;
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
