<template>
    <span>
        <span v-if="statusRight">
            <span v-if="testStatus[name] === 'pass'">
                <img src="../../assets/checked.png" class="align-right">
            </span>
            <span v-else-if="testStatus[name] === 'fail'">
                <img src="../../assets/error.png" class="align-right">
            </span>
            <span v-else-if="testStatus[name] === 'error'">
                <img src="../../assets/yellow-error.png" class="align-right">
            </span>
            <span v-else>
                <img src="../../assets/blank-circle.png" class="align-right">
            </span>
        </span>
        <span v-if="!statusRight">
            <span v-if="testStatus[name] === 'pass'">
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
