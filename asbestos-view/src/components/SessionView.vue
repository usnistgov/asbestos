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
        },
        computed: {
        },
        components: {

        },
        watch: {
        },
        props: [
            'sessionId'
        ],
        name: "SessionView"
    }
</script>

<style scoped>
</style>
