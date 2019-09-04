<template>
    <div>
        <span class="tool-title">Tests for Channel {{ testCollection }}</span>
        <span class="divider"></span>

        <img id="reload" class="selectable" @click="reload()" src="../assets/reload.png"/>
        <span class="divider"></span>

        <div v-for="(name, i) in testScriptNames"
             :key="name + i">
            <div >
                <div @click="selectTest(name)">
                    <div v-bind:class="{ pass: pass(name), fail: fail(name), 'not-run': notRun(name)   }">
                        <div v-if="pass(name)">
                            <img src="../assets/checked.png" class="right">
                        </div>
                        <div v-else-if="fail(name)">
                            <img src="../assets/error.png" class="right">
                        </div>
                        <div v-else>
                            <img src="../assets/blank-circle.png" class="right">
                        </div>
                        <img src="../assets/press-play-button.png" class="right" @click.stop="doRun(name)">
                        {{ name }}
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
                testScriptNames: [],
                selected: null,  // name
            }
        },
        methods: {
            doRun(testName) {
 //               console.log(`run ${testName}`)
                const that = this
                ENGINE.post(`testrun/${this.sessionId}__${this.channelId}/${this.testCollection}/${testName}`)
                    .then(response => {
//                        console.log(`response => ${response.data.result}`)
                        this.$store.commit('addTestReport', { name: testName, report: response.data } )
                    })
                    .catch(error => {
                        that.error(error)
                    })
            },
            selectTest(name) {
                if (this.selected === name) {
                    this.selected = null
                    const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}`
 //                   console.log(`route to ${route}`)
                    this.$router.push(route)
                    return
                }
                this.selected = name
                const route = `/session/${this.sessionId}/channel/${this.channelId}/collection/${this.testCollection}/test/${name}`
//                console.log(`route to ${route}`)
                this.$router.push(route)
            },
            loadTestScriptNames() {
                const that = this
                ENGINE.get(`collection/${this.testCollection}`)
                    .then(response => {
                        let theResponse = response.data
 //                       console.info(`TestEnginePanel: loaded ${theResponse.length} test script names`)
                        this.testScriptNames = theResponse
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
            },
            loadReports() {
                const that = this
                ENGINE.get(`testlog/${this.sessionId}__${this.channelId}/${this.testCollection}`)
                    .then(response => {
                        this.$store.commit('clearTestReports')
                        for (const reportName of Object.keys(response.data)) {
                            const report = response.data[reportName]
//                            console.log(`${reportName} is ${report.result}`)
                            this.$store.commit('addTestReport', {name: reportName, report: report})
                        }
                    })
                    .catch(function (error) {
                        that.error(error)
                    })
            },
            reload() {
                this.loadTestScriptNames()
                this.loadReports()
            },
            pass(testName) {
                return this.$store.state.base.testReports[testName] !== undefined && this.$store.state.base.testReports[testName].result === 'pass'
            },
            fail(testName) {
                return this.$store.state.base.testReports[testName] !== undefined && this.$store.state.base.testReports[testName].result === 'fail'
            },
            notRun(testName) {
                return this.$store.state.base.testReports[testName] === undefined
            },
        },
        computed: {
            current() {
                return this.$store.state.base.testCollectionDetails.find(item => {
                    return item.name === this.testId
                })
            },
        },
        created() {
            this.reload()
        },
        mounted() {

        },
        watch: {

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
    }
    .fail {
        background-color: indianred;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
    }
    .not-run {
        background-color: lightgray;
        text-align: left;
        border: 1px dotted black;
        cursor: pointer;
    }
    .right {
        text-align: right;
    }
</style>
