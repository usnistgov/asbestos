<template>
    <div>
        <span class="tool-title">Tests for Collection {{ testCollection }}</span>
        <span class="divider"></span>

        <img id="reload" class="selectable" @click="reload()" src="../assets/reload.png"/>
        <span class="divider"></span>

        <div v-for="(name, i) in $store.state.testRunner.testScriptNames"
             :key="name + i">
            <div >
                <div @click="selectTest(name)">
                    <div v-bind:class="{ pass: status[name] === 'pass', fail: status[name] === 'fail', error: status[name] === 'error', 'not-run': status[name] === 'undefined' }">
                        <div v-if="status[name] === 'pass'">
                            <img src="../assets/checked.png" class="right">
                        </div>
                        <div v-else-if="status[name] === 'fail' || status[name] === 'error'">
                            <img src="../assets/error.png" class="right">
                        </div>
                        <div v-else>
                            <img src="../assets/blank-circle.png" class="right">
                        </div>
                        <img src="../assets/press-play-button.png" class="right" @click.stop="doRun(name)">
                        {{ name }}  --  {{ time[name] }}
                    </div>
                </div>
                <div v-if="selected === name">
                    <router-view></router-view>
                </div>
            </div>
        </div>

    </div>
</template>

<script>
    import {ENGINE} from '../common/http-common'
    import errorHandlerMixin from '../mixins/errorHandlerMixin'

    export default {

        data() {
            return {
                status: [],   // testName => undefined, 'pass', 'fail', 'error'
                time: [],
            }
        },
        methods: {
            doRun(testName) {
                this.$store.commit('setCurrentTest', null)
                const that = this
                ENGINE.post(`testrun/${this.sessionId}__${this.channelId}/${this.testCollection}/${testName}`)
                    .then(response => {
                        this.$store.dispatch('addTestReport', testName, response.data)
                        this.$router.replace(`/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`)
                        this.$store.dispatch('loadTestScriptNames')  // force reload of UI
                    })
                    .catch(error => {
                        that.error(error)
                    })
            },
            selectTest(name) {
                if (this.selected === name)  { // unselect
                    this.$store.commit('setCurrentTest', null)
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`
                    this.$router.push(route)
                    return
                }
                this.$store.commit('setCurrentTest', name)
                const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${name}`
                this.$router.push(route)
            },
            reload() {
                this.$store.commit('setTestCollectionName', this.testCollection)
                this.$store.dispatch('loadTestScriptNames')
                this.$router.push(`/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`)
            },
            testReport(testName) {
                return this.$store.state.testRunner.testReports[testName]
            },
            updateReportStatuses() {
                console.log('TestCollection: UpdateReportStatuses')
                let status = []
                let time = []
                this.allTestScriptNames().forEach(testName => {
                    if (this.testReport(testName) === undefined) {
                        status[testName] = 'undefined'
                    } else {
                        status[testName] = this.testReport(testName).result  // 'pass', 'fail', 'error'
                        time[testName] = this.testReport(testName).issued
                    }
                    this.status = status
                    this.time = time
                })
            },
            loadReports() {
                this.$store.dispatch('loadReports')
            },
            allTestScriptNames() {
                return this.$store.state.testRunner.testScriptNames
            },
        },
        computed: {
            current() {
                return this.$store.state.base.testCollectionDetails.find(item => {
                    return item.name === this.testId
                })
            },
            selected() {
                return this.$store.state.testRunner.currentTest
            },

            testScriptNames() {  // just the ones with reports available
                const reports = this.$store.state.testRunner.testReports
                return Object.keys(reports).sort()
            },
            channel: {
                set(name) {
                    if (name !== this.$store.state.base.channelId) {
                        this.$store.commit('setChannelId', name)
                    }
                },
                get() {
                    return this.$store.state.base.channelId
                }
            },

        },
        created() {
            this.reload()
            this.channel = this.channelId
        },
        mounted() {

        },
        watch: {
            'testCollection': 'loadReports',
            'channelId': function(newVal) {
                if (this.channel !== newVal)
                    this.channel = newVal
            },
            '$store.state.testRunner.testScriptNames' : 'loadReports',
            '$store.state.testRunner.testReports': 'updateReportStatuses',
        },
        mixins: [ errorHandlerMixin ],
        name: "TestList",
        props: [
            'sessionId', 'channelId', 'testCollection',
        ]
    }
</script>

<style scoped>
    .banner-color {
        background-color: lightgray;
        text-align: left;
    }
    .pass {
        background-color: lightgreen;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .fail {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .error {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .not-run {
        background-color: lightgray;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
        /*font-size: larger;*/
    }
    .right {
        text-align: right;
    }
</style>
