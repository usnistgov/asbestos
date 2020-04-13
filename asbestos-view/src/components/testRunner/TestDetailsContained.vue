<template>
    <div>
        <div v-if="script" class="conditional-margins">
<!--            <span v-bind:class="{-->
<!--                'not-run': isNotRun,-->
<!--                error: isError,-->
<!--                'condition-fail': isConditionFailed,-->
<!--                pass : isPass,-->
<!--                fail: isFail}"  class="test-margins" @click.stop="toggleDisplay()">-->

<!--                <span v-if="script.description">-->
<!--                    <span class="bold">{{ script.description }} </span>-->
<!--                </span>-->
<!--                <span v-else>-->
<!--                    <span class="bold">Then </span>-->
<!--                </span>-->
<!--            </span>-->

            <div v-if="displayOpen" @click.stop="toggleDisplay()">
                <div v-for="(action, actioni) in script.action"
                     :key="'Action' + actioni">
                    <action-details-contained
                            :script="action"
                            :report="report ? report.action[actioni] : null"> </action-details-contained>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import ActionDetailsContained from './ActionDetailsContained'
    import colorizeTestReports from "../../mixins/colorizeTestReports";

    export default {
        data() {
            return {
                displayOpen: true,  // start closed
            }
        },
        methods: {
            toggleDisplay() {
                this.displayOpen = !this.displayOpen
            },
        },
        computed: {
            hasOperation() {
                const test = this.script
                let hasOperation = false
                test.action.forEach(action => {
                    hasOperation = hasOperation || action.operation
                })
                return hasOperation
            },
        },
        created() {
        },
        props: [
            // parts representing a single test element of a contained TestScript
            'script', 'report', 'description',
        ],
        components: {
            ActionDetailsContained,
        },
        mixins: [colorizeTestReports],
        name: "TestDetailsContained"
    }
</script>

<style scoped>

</style>
