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
                // return this.testReport.result
                return JSON.stringify(this.testReport, null, 4)
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
        ],
        name: "EvalReportEvent"
    }
</script>

<style scoped>
.detail {
    background-color: white;
}
</style>
