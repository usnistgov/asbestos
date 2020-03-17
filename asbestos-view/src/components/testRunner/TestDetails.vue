<template>
    <div>
        <div v-if="script">
            <div v-bind:class="{
                'not-run': isNotRun,
                error: isError,
                pass : isPass,
                fail: isFail}"  class="test-margins" @click="toggleDisplay()">
                <span>
                    <span v-if="label" class="bold">{{label}}: </span>
                    <span v-else class="bold">Test: </span>
                    {{ description }}
                </span>
            </div>

            <div v-if="isConditional" class="conditional-margins">
                <div>
                    <script-details-contained
                            :script="scriptConditional"
                            :report="reportConditional"
                    > </script-details-contained>
                </div>
            </div>

            <div v-if="display">
                <div v-for="(action, actioni) in script.action" class="action-margins"
                     :key="'Action' + actioni">
                    <action-details
                            :script="action"
                            :report="report && report.action ? report.action[actioni] : null"> </action-details>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import ActionDetails from './ActionDetails'
    import ScriptDetailsContained from "./ScriptDetailsContained";
    import colorizeTestReports from "../../mixins/colorizeTestReports";

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
            scriptConditional() { // TestScript representing conditional
                if (!this.scriptConditionalRef) return null
                let conditional = null
                this.scriptContained.forEach(contained => {
                    if (contained.id === this.scriptConditionalRef)
                        conditional = contained
                })
                return conditional
            },
            reportConditional() { // TestReport representing conditional
                if (!this.scriptConditionalRef) return null
                let conditional = null
                if (this.reportContained) {
                    this.reportContained.forEach(contained => {
                        if (contained.id === this.scriptConditionalRef && contained.resourceType === 'TestReport')
                            conditional = contained
                    })
                }
                return conditional
            },
            isConditional() {
                return this.script.modifierExtension && this.script.modifierExtension[0].url === 'https://github.com/usnistgov/asbestos/wiki/TestScript-Conditional'
            },
            scriptConditionalRef() {
                if (!this.script.modifierExtension) return null
                const ref = this.script.modifierExtension[0].valueReference.reference  // [0] => there can be only one
                if (ref) return ref.substring(1)  // ref without preceding #
                return null
            },

            description() {
                if (!this.script) return ""
                return this.script.description
            },
        },
        created() {
        },
        props: [
            // parts representing a single test element of a TestScript
            'script', 'report',
            'scriptContained', 'reportContained', // contained section of the TestScript and TestReport
            'label',
        ],
        components: {
            ActionDetails, ScriptDetailsContained,
        },
        mixins: [colorizeTestReports],
        name: "TestDetails"
    }
</script>

<style scoped>

</style>
