<template>
    <div @click="toggleDisplay">
        Event {{ eventId }}
        <div v-if="display" class="detail">
            {{ eventDetail }}
        </div>
    </div>
</template>

<script>
    export default {
        data() {
            return {
                display: false,
            }
        },
        methods: {
            toggleDisplay() {
                this.display = !this.display
            },
        },
        computed: {
            eventDetail() {
                const theAssert = this.testReport.test[0].action[this.actionIndex].assert
                return `${theAssert.result} - ${theAssert.message}`
            },
        },
        created() {

        },
        mounted() {

        },
        watch: {
        },
        props: [
            'eventId',
            'testReport',  // TestReport for single eventId
            'actionIndex', // reporting on a single action/assert in the testReport
        ],
        name: "EvalReportEvent"
    }
</script>

<style scoped>
.detail {
    background-color: white;
}
</style>
