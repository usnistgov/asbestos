<template>
    <div>
<!--        <br />-->
        <div v-if="script">
            <div class="pre-test-gap"></div>
            <span v-bind:class="{
                pass:         isPass  && colorful,
                'pass-plain-detail': isPass  && !colorful,
                fail:         isFail  && colorful,
                'fail-plain-detail': isFail  && !colorful,
                 error:       isError && colorful,
                 'error-plain': isError && !colorful,
                 'not-run':   isNotRun && colorful,
                 'not-run-plain-detail' : isNotRun && ! colorful,
            }"  class="test-margins" @click="toggleDisplay()">

                <test-status v-if="!statusRight"
                             :status-on-right="statusRight"
                             :report="report"
                > </test-status>

            <span v-if="display">
                <img src="../../assets/arrow-down.png">
            </span>
            <span v-else>
                <img src="../../assets/arrow-right.png"/>
            </span>

                <span>
                    <span v-if="label" class="bold">{{label}}: </span>
                    <span v-else class="bold">Test: </span>
                    {{ description }}
                </span>
            </span>

            <test-status v-if="statusRight"
                         :status-on-right="statusRight"
                         :report="report"
            > </test-status>


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
        <br />
    </div>
</template>

<script>
    import ActionDetails from './ActionDetails'
    import ScriptDetailsContained from "./ScriptDetailsContained";
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import TestStatus from "./TestStatus";

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
            ActionDetails, ScriptDetailsContained, TestStatus
        },
        mixins: [colorizeTestReports],
        name: "TestDetails"
    }
</script>

<style scoped>

</style>
