<template>
    <div>
        <h1>Asbestos</h1>
        <router-view name="session"></router-view>
<!--        <session-view></session-view>-->
<!--        <router-view></router-view>-->
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
<style scoped>
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
</style>
