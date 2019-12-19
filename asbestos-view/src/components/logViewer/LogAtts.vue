<template>
    <div>
        <div class="caption">Attributes</div>
        <log-atts-inter class="atts" :attMap="attMap"> </log-atts-inter>
    </div>
</template>

<script>
    import LogAttsInter from './LogAttsInter'
    export default {
        data() {
            return {
            }
        },
        methods: {
        },
        props: [
            'attMap'
        ],
        components: { LogAttsInter },
        name: "LogAtts"
    }
</script>

<style scoped>
    .atts {
        /*background-color: #ddd;*/
    }
    .details {
        font-size: smaller;
    }
    .container {
        display: grid;
    }
    .container name {
        grid-column: 1;
    }
    .container value {
        grid-column: 2;
    }
    .container > * {
        background-color: #ddd;
    }
    .caption {
        font-weight: bold;
    }
</style>
