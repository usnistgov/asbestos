<template>
    <div>
        <div @click.self="select()" v-bind:class="{
                                    pass: result === 'pass' && colorful,
                                    'pass-plain': result === 'pass' && !colorful,
                                    fail: result === 'fail' && colorful,
                                    'fail-plain': result === 'fail' && !colorful,
                                    error: result === 'error' && colorful,
                                    'error-plain': result === 'error' && !colorful,
                                    warning: result === 'warning' && colorful,
                                    'not-run': result === 'not-run'  && colorful
                            }">

            <test-status v-if="!statusRight"
                         :status-on-right="statusRight"
                         :report="report"
            > </test-status>

            <span class="selectable" @click.self="select()">Assert:  {{ description }}</span>

            <span v-if="open">
                <img src="../../assets/arrow-down.png" @click.self="select()">
            </span>
            <span v-else>
                <img src="../../assets/arrow-right.png" @click.self="select()">
            </span>
        </div>

        <div v-if="open">
            <div v-if="message && message.indexOf('#') === -1">
                <ul>
                    <div v-for="(line, linei) in translateNL(message)"
                         :key="'msgDisp' + linei">
                        <li>
                            {{ line }}
                        </li>
                    </div>
                </ul>
            </div>
            <div v-else>
                No Evaluation
            </div>

            <!-- Test Script/Report -->
            <div>
                <span class="selectable" @click="toggleScriptDisplayed()">Test Script/Report</span>
                <span v-if="displayScript">
                    <img src="../../assets/arrow-down.png" @click="toggleScriptDisplayed()">
                   <vue-markdown v-if="message">{{message}}</vue-markdown>
                    <script-display
                            :script="script"
                            :report="report">
                    </script-display>
                </span>
                <span v-else>
                    <img src="../../assets/arrow-right.png" @click="toggleScriptDisplayed()">
                </span>
            </div>
        </div>
    </div>
</template>

<script>
    import colorizeTestReports from "../../mixins/colorizeTestReports";
    import TestStatus from "./TestStatus";
    import VueMarkdown from 'vue-markdown';
    import ScriptDisplay from "./ScriptDisplay";

    export default {
        data() {
            return {
                open: false,
                displayScript: false,
            }
        },
        methods: {
            toggleScriptDisplayed() {
                this.displayScript = !this.displayScript
            },
            translateNL(string) {
                if (!string)
                    return string
                return string.replace(/\t/g, "  ").split('\n')
            },
            select() {
                this.open = !this.open;
            }
        },
        computed: {
            message() {
                return this.report.assert.message
            },
            result() {
                return this.report.assert.result;
            },
            description() {
                const rawDesc = this.script.assert.description;
                if (!rawDesc.includes("|"))
                    return rawDesc;
                const elements = rawDesc.split("|");
                return elements[0];
            }
        },
        components: {
            TestStatus,
            VueMarkdown,
            ScriptDisplay
        },
        mixins: [ colorizeTestReports ],
        props: [
            'script', 'report'    // action parts
        ],
        name: "EvalActionDetails"
    }
</script>

<style scoped>

</style>
