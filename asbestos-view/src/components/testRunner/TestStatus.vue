<template>
    <span>
        <span v-if="statusOnRight">
            <span v-if="isPass">
                <img src="../../assets/checked.png" class="align-right">
            </span>
            <span v-else-if="isFail || isError">
                <img src="../../assets/error.png" class="align-right">
            </span>
            <span v-else>
                <img src="../../assets/blank-circle.png" class="align-right">
            </span>
        </span>
        <span v-if="!statusOnRight">
            <span v-if="isPass">
                <img src="../../assets/checked.png" class="align-left">
            </span>
            <span v-else-if="isFail || isError">
                <img src="../../assets/error.png" class="align-left">
            </span>
            <span v-else>
                <img src="../../assets/blank-circle.png" class="align-left">
            </span>
        </span>
    </span>
</template>

<script>
    import colorizeTestReports from "../../mixins/colorizeTestReports";

    export default {
        computed: {
        },
        props: [
            'statusOnRight', 'report'
        ],
        mixins: [colorizeTestReports],
        name: "TestStatus"
    }
</script>

<style scoped>

</style>
