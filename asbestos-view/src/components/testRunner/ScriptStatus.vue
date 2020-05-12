<template>
    <span>
            <span v-if="testStatus.length === 0">
                <img src="../../assets/question-button.png" class="align-left">
            </span>
            <span v-else-if="testStatus[name] === 'pass'">
                <img src="../../assets/checked.png" class="align-left">
            </span>
            <span v-else-if="testStatus[name] === 'fail'">
                <img src="../../assets/error.png" class="align-left">
            </span>
            <span v-else-if="testStatus[name] === 'error'">
                <img src="../../assets/yellow-error.png" class="align-left">
            </span>
            <span v-else>
                <img src="../../assets/blank-circle.png" class="align-left">
            </span>
    </span>
</template>

<script>
    export default {
        computed: {
            testStatus() {
                return this.$store.getters.testStatus
            }
        },
        props: [
            'statusRight', 'name'
        ],
        name: "ScriptStatus"
    }
</script>

<style scoped>

</style>
