<template>
    <div class="container">
        <channel-nav :session-id="sessionId" class="nav"></channel-nav>
        <channel-edit :sessionId="sessionId" :channelId="channelId" class="view"></channel-edit>
    </div>
</template>

<script>
    import ChannelNav from "./ChannelNav"
    import ChannelEdit from "./ChannelEdit"

    export default {
        data() {
            return {

            }
        },
        components: {
            ChannelNav,
            ChannelEdit
        },
        name: "ChannelsView",
        mounted() {
        },
        methods: {
        },
        props: [
           'sessionId', 'channelId'
        ]
    }
</script>

<style scoped>
    .container {
        display: grid;
        grid-template-columns: auto auto;
        grid-template-areas: 'nav view';
        align-content: start;
    }
    .nav {
        grid-area: nav;
        border: 1px dotted black;
        text-align: left;
        width: fit-content;
    }
    .view {
        grid-area: view;
        text-align: left;
        width: fit-content;
        /*border: 1px dotted black;*/
    }

</style>
