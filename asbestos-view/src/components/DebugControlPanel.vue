<template>
    <div>
        <div>Debug</div>
        <div class="bold">currentTestCollectionName</div>{{ $store.state.testRunner.currentTestCollectionName }}
        <div class="bold">currentTest</div>{{ $store.state.testRunner.currentTest }}
        <div class="bold">currentAssertIndex</div>{{ $store.state.testRunner.currentAssertIndex }}
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
        computed: {

        },
        created() {

        },
        mounted() {

        },
        watch: {

        },
        name: "DebugControlPanel"
    }
</script>

<style scoped>

</style>
