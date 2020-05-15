<template>
    <div>
        <div v-if="script" class="script">
            <div v-if="script.description && script.description !== ''" class="script-description-margins">
               <vue-markdown>{{ description }}</vue-markdown>
            </div>
            <div v-else class="grayText">
                (No description.)
            </div>
            <div v-if="systemError" class="system-error">
                {{systemError}}
            </div>
            <div v-if="displayDetail">
                <div v-for="(fixture, i) in fixtures"
                     :key="i">
                    <span class="name" >Fixture: </span>
                    <span class="value">{{ fixture.id }}</span>
                </div>
                <div v-for="(variable, i) in variables"
                     :key="'Var' + i">
                    <span class="name" >Variable: </span>
                    <span class="value">{{ variable.name }}</span>
                </div>
            </div>

            <div v-if="script.setup">
                <ul class="noListStyle">
                    <li
                            :key="'Setup0'"
                            v-bind:class="{
                                'breakpoint-indicator': showSetupBreakpointIndicator(testScriptIndex, 0),
                                'breakpoint-hit-indicator': isBreakpointHit(testScriptIndex, 'setup', 0),
                            }"
                    >
                    <test-details
                        :script="script.setup"
                        :report="report ? report.setup : null"
                        :label="'Setup'"
                        :script-contained="script.contained"
                        :report-contained="report ? report.contained : null"
                        :test-script-index="testScriptIndex"
                        :test-type="'setup'"
                        :test-index="0"
                        :test-level-debug-title="debugTitle(testScriptIndex, 'setup', 0)"
                        @onStatusMouseOver="hoverSetupLevelIndex = 0"
                        @onStatusMouseLeave="hoverSetupLevelIndex = -1"
                        @onStatusClick="toggleBreakpointIndex(testScriptIndex, 'setup', 0)"
                    ></test-details>
                    </li>
                </ul>
            </div>

            <div v-if="!script.setup">
                <action-details
                        :script="null"
                        :report="report && report.setup ? report.setup.action : null">
                </action-details>
            </div>
<!--            <div v-else>-->
<!--                <action-details-->
<!--                        :script="null"-->
<!--                        :report="report && report.setup ? report.setup.action : null">-->
<!--                </action-details>-->
<!--            </div>-->

            <div v-if="script.test">
                    <ul class="noListStyle">
                        <li
                           v-for="(test, testi) in tests"
                           :key="'Test' + testi"
                           v-bind:class="{
                                'breakpoint-indicator': showTestBreakpointIndicator(testScriptIndex, 'test', testi),
                                'breakpoint-hit-indicator': isBreakpointHit(testScriptIndex, 'test', testi),
                            }"
                        >
                            <test-details
                                :script="script.test[testi]"
                                :report="report && report.test  ? report.test[testi] : null"
                                :script-contained="script.contained"
                                :report-contained="report ? report.contained : null"
                                :test-script-index="testScriptIndex"
                                :test-type="'test'"
                                :test-index="testi"
                                :test-level-debug-title="debugTitle(testScriptIndex, 'test', testi)"
                                @onStatusMouseOver="hoverTestLevelIndex = testi"
                                @onStatusMouseLeave="hoverTestLevelIndex = -1"
                                @onStatusClick="toggleBreakpointIndex(testScriptIndex, 'test', testi)"
                            ></test-details>
                        </li>
                    </ul>
            </div>

            <!-- add TEARDOWN here -->

        </div>
    </div>
</template>

<script>
    import errorHandlerMixin from '../../mixins/errorHandlerMixin'
    import TestDetails from "./TestDetails";
    // import ActionDetails from "./ActionDetails";
    import VueMarkdown from "vue-markdown";

    export default {
        data() {
            return {
                displayDetail: false,
                hoverTestLevelIndex: -1,
                hoverSetupLevelIndex: -1,
            }
        },
        methods: {
            async toggleBreakpointIndex(testScriptIndex, testType, testIndex) {
                // console.log("enter toggleBreakpointIndex")
                let obj = {testScriptIndex: testScriptIndex, breakpointIndex: testType + testIndex }
                if (! this.$store.getters.hasBreakpoint(obj)) {

                    if (testType === 'test') {
                        this.hoverTestLevelIndex = testIndex // Restore the hoverActionIndex when toggle on the same item goes from on(#)-off(-1)-on(#)
                    } else if (testType === 'setup') {
                        this.hoverSetupLevelIndex = testIndex
                    }
                    await this.$store.dispatch('addBreakpoint', obj)
                } else {
                    if (testType === 'test') {
                        this.hoverTestLevelIndex = -1 // Immediately remove the debug indicator while the mouse hover is still active but without having to wait for the mouseLeave event
                    } else if (testType === 'setup') {
                        this.hoverSetupLevelIndex = -1
                    }
                    // remove breakpoint
                    await this.$store.dispatch('removeBreakpoint', obj)
                }
            },
            debugTitle(testScriptIndex, testType, testIndex) {
                let obj = {testScriptIndex: testScriptIndex, breakpointIndex: testType + testIndex }
                return this.$store.getters.getDebugTitle(obj);
            },
            showSetupBreakpointIndicator(testScriptIndex, setupIndex) {
                let obj = {testScriptIndex: testScriptIndex, breakpointIndex: 'setup0' }
                let hasBreakpoint = this.$store.getters.hasBreakpoint(obj)
                let isHover = setupIndex === this.hoverSetupLevelIndex
                let isBreakpointHit = this.isBreakpointHit(testScriptIndex, 'setup', 0)
                return (hasBreakpoint || isHover) && ! isBreakpointHit // Vue Reactivity seems to work better when boolean logic is written like this. Otherwise this whole method showSetup... does not even get called for unknown reason.
            },
            showTestBreakpointIndicator(testScriptIndex, testType, testIndex) {
                // console.log('calling showTestBreakpointIndicator ' + this.hoverTestLevelIndex)
                let obj = {testScriptIndex: testScriptIndex, breakpointIndex: testType + testIndex }
                return (this.$store.getters.hasBreakpoint(obj) || this.hoverTestLevelIndex === testIndex) && ! this.isBreakpointHit(testScriptIndex, testType, testIndex)
            },
            isBreakpointHit(testScriptIndex, testType, testIndex) {
                let obj = {testScriptIndex: testScriptIndex, breakpointIndex: testType + testIndex}
                return this.$store.getters.isBreakpointHit(obj)
            },
        },
        computed: {
            systemError() {
                if (!this.report) return null;
                if (!this.report.extension) return null;
                let error = null
                this.report.extension.forEach(extension => {
                    if (extension.url === 'urn:failure')
                        error = extension.valueString;
                })
                return error
            },
            description() {
                return this.script.description.replace(/\\n\\n/g, "<br />").replace(/\\n/g, " ")
            },
            fixtures() {
                return this.script.fixture
            },
            variables() {
                return this.script.variable
            },
            tests() {
                return this.script.test
            },
        },
        created() {
        },
        mounted() {

        },
        watch: {
        },
        mixins: [ errorHandlerMixin ],
        props: [
            'script', 'report',  // TestScript and TestReport
            'testScriptIndex',
        ],
        components: {
            TestDetails,
            // ActionDetails,
            VueMarkdown,
        },
        name: "ScriptDetails"
    }
</script>

<style scoped>
    .script {
        text-align: left;
    }
    .name {
        font-weight: bold;
    }
    .value {

    }

</style>
<style>

    /*
    .test-margins {
        margin-left: 0px;
        margin-right: 0px;
    }
    .action-margins {
        margin-left: 20px;
        margin-right: 20px;
    }
    .conditional-margins {
        margin-left: 40px;
        margin-right: 40px;
    }
    .script-description-margins {
        margin-left: 30px;
        margin-right: 30px;
    }
     */
</style>
