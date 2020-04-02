<template>
    <div>
        <div v-if="script">
            <div v-bind:class="{
                'not-run': isNotRun,
                error: isError,
                'condition-fail': isConditionFailed,
                pass : isPass,
                fail: isFail}"  class="test-margins" @click="toggleConditionDisplay()">
                <span v-if="hasOperation">
                    <span class="bold">Condition Context: </span> <!--{{ description }}-->
                </span>
                <span v-else>
                    <span class="bold">Condition: </span> <!--{{ description }}-->
                </span>
            </div>

            <div v-if="displayCondition" @click="toggleThenClauseDisplay()">
                <div v-for="(action, actioni) in script.action" class="action-margins"
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
                displayThenClause: false,
                displayCondition: false,
            }
        },
        methods: {
            toggleThenClauseDisplay() {
                this.displayThenClause = !this.displayThenClause
            },
            toggleConditionDisplay() {
                this.displayCondition = !this.displayCondition
            }
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
