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
            this.$store.dispatch('loadSessions')
            this.$store.commit('setSession', this.sessionId)
        },
        computed: {
        },
        components: {

        },
        watch: {
            sessionId: function(newVal, oldVal) {
                if (newVal !== oldVal)
                    this.$store.commit('setSession', newVal)
            }
        },
        props: [
            'sessionId'
        ],
        name: "SessionView"
    }
</script>

<style scoped>
</style>
