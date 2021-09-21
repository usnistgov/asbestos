<template>
    <div class="panel">
        <div class="panel">
            <router-view></router-view>
        </div>
    </div>
</template>

<script>

    export default {
        data() {
            return {
            }
        },
        methods: {
        },
        created() {
            //this.$store.dispatch('loadSessions')
            /*
            Part 1. Is this really needed since the session is already set in initSessionsStore() base.js?
            const current = this.$router.currentRoute.path;
            const parts = current.split("/");
            const size = parts.length;
            // https://fhirtoolkit.test:8082/session/default/channel/default/collection/Test_Documents
            if (!(size === 9  && parts[3] === 'session' && parts[5]==='channel' ))
            {
                console.log('About to setSession')
                this.$store.commit('setSession', this.sessionId)
            }
             */
        },
        computed: {
        },
        components: {

        },
        watch: {
            /*
            Part 2. Is this really needed since the session is already set in initSessionsStore() base.js?
            sessionId: function(newVal, oldVal) {
                if (newVal !== oldVal)
                    this.$store.commit('setSession', newVal)
            }
            *
             */
        },
        props: [
            'sessionId'
        ],
        name: "SessionView"
    }
</script>

<style scoped>
</style>
